/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyThread.Status;
import org.jruby.exceptions.MainExitException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.cast.NumericToFloatNode;
import org.jruby.truffle.nodes.cast.NumericToFloatNodeGen;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.core.KernelNodesFactory.CopyNodeFactory;
import org.jruby.truffle.nodes.core.KernelNodesFactory.SameOrEqualNodeFactory;
import org.jruby.truffle.nodes.core.KernelNodesFactory.SingletonMethodsNodeFactory;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.globals.WrapInThreadLocalNode;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.nodes.rubinius.ObjectPrimitiveNodes;
import org.jruby.truffle.nodes.rubinius.ObjectPrimitiveNodesFactory;
import org.jruby.truffle.pack.parser.FormatParser;
import org.jruby.truffle.pack.runtime.PackResult;
import org.jruby.truffle.pack.runtime.exceptions.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyModule.MethodFilter;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.FeatureManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.util.ByteList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@CoreClass(name = "Kernel")
public abstract class KernelNodes {

    @CoreMethod(names = "`", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class BacktickNode extends CoreMethodArrayArgumentsNode {

        public BacktickNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString backtick(RubyString command) {
            // Command is lexically a string interoplation, so variables will already have been expanded

            CompilerDirectives.transferToInterpreter();

            final RubyContext context = getContext();

            final RubyHash env = context.getCoreLibrary().getENV();

            final List<String> envp = new ArrayList<>();

            // TODO(CS): cast
            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(env)) {
                envp.add(keyValue.getKey().toString() + "=" + keyValue.getValue().toString());
            }

            final Process process;

            try {
                // We need to run via bash to get the variable and other expansion we expect
                process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command.toString()}, envp.toArray(new String[envp.size()]));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final InputStream stdout = process.getInputStream();
            final InputStreamReader reader = new InputStreamReader(stdout, StandardCharsets.UTF_8);

            final StringBuilder resultBuilder = new StringBuilder();

            // TODO(cs): this isn't great for binary output

            try {
                int c;

                while ((c = reader.read()) != -1) {
                    resultBuilder.append((char) c);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // TODO (nirvdrum 10-Mar-15) This should be using the default external encoding, rather than hard-coded to UTF-8.
            return context.makeString(resultBuilder.toString(), RubyEncoding.getEncoding("UTF-8").getEncoding());
        }

    }

    /**
     * Check if operands are the same object or call #==.
     * Known as rb_equal() in MRI. The fact Kernel#=== uses this is pure coincidence.
     */
    @CoreMethod(names = "===", required = 1)
    public abstract static class SameOrEqualNode extends CoreMethodArrayArgumentsNode {

        @Child private BasicObjectNodes.ReferenceEqualNode referenceEqualNode;
        @Child private CallDispatchHeadNode equalNode;

        private final ConditionProfile sameProfile = ConditionProfile.createBinaryProfile();

        public SameOrEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract boolean executeSameOrEqual(VirtualFrame frame, Object a, Object b);

        @Specialization
        public boolean sameOrEqual(VirtualFrame frame, Object a, Object b) {
            if (sameProfile.profile(areSame(frame, a, b))) {
                return true;
            } else {
                return areEqual(frame, a, b);
            }
        }

        private boolean areSame(VirtualFrame frame, Object left, Object right) {
            if (referenceEqualNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                referenceEqualNode = insert(BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(getContext(), getSourceSection(), null, null));
            }

            return referenceEqualNode.executeReferenceEqual(frame, left, right);
        }

        private boolean areEqual(VirtualFrame frame, Object left, Object right) {
            if (equalNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                equalNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), false, false, null));
            }

            return equalNode.callBoolean(frame, left, "==", null, right);
        }

    }

    @CoreMethod(names = "=~", required = 1, needsSelf = false)
    public abstract static class MatchNode extends CoreMethodArrayArgumentsNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject equal(Object other) {
            return nil();
        }

    }

    @CoreMethod(names = "!~", required = 1)
    public abstract static class NotMatchNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode matchNode;

        public NotMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            matchNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        @Specialization
        public boolean notMatch(VirtualFrame frame, Object self, Object other) {
            return !matchNode.callBoolean(frame, self, "=~", null, other);
        }

    }

    @CoreMethod(names = {"<=>"}, required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        @Child private SameOrEqualNode equalNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
        }

        @Specialization
        public Object compare(VirtualFrame frame, Object self, Object other) {
            if (equalNode.executeSameOrEqual(frame, self, other)) {
                return 0;
            } else {
                return nil();
            }
        }

    }

    @CoreMethod(names = "abort", isModuleFunction = true)
    public abstract static class AbortNode extends CoreMethodArrayArgumentsNode {

        public AbortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject abort() {
            getContext().innerShutdown(false);
            throw new MainExitException(1, true);
        }
    }

    @CoreMethod(names = "binding", isModuleFunction = true)
    public abstract static class BindingNode extends CoreMethodArrayArgumentsNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract RubyBinding executeRubyBinding(VirtualFrame frame);

        public Object execute(VirtualFrame frame) {
            return executeRubyBinding(frame);
        }

        @Specialization
        public RubyBinding binding() {
            // Materialize the caller's frame - false means don't use a slow path to get it - we want to optimize it

            final MaterializedFrame callerFrame = Truffle.getRuntime().getCallerFrame()
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();

            return new RubyBinding(
                    getContext().getCoreLibrary().getBindingClass(),
                    RubyArguments.getSelf(callerFrame.getArguments()),
                    callerFrame);
        }
    }

    @CoreMethod(names = "block_given?", isModuleFunction = true)
    public abstract static class BlockGivenNode extends CoreMethodArrayArgumentsNode {

        public BlockGivenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean blockGiven() {
            return RubyArguments.getBlock(Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_ONLY, false).getArguments()) != null;
        }
    }

    @CoreMethod(names = "__callee__", needsSelf = false)
    public abstract static class CalleeNameNode extends CoreMethodArrayArgumentsNode {

        public CalleeNameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol calleeName(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();
            // the "called name" of a method.
            return getContext().getSymbolTable().getSymbol(RubyCallStack.getCallingMethod(frame).getName());
        }
    }

    @CoreMethod(names = "caller_locations", isModuleFunction = true, optional = 2, lowerFixnumParameters = {0, 1})
    public abstract static class CallerLocationsNode extends CoreMethodArrayArgumentsNode {

        public CallerLocationsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray callerLocations(UndefinedPlaceholder undefined1, UndefinedPlaceholder undefined2) {
            return callerLocations(1, -1);
        }

        @Specialization
        public RubyArray callerLocations(int omit, UndefinedPlaceholder undefined) {
            return callerLocations(omit, -1);
        }

        @TruffleBoundary
        @Specialization
        public RubyArray callerLocations(int omit, int length) {
            final RubyClass threadBacktraceLocationClass = getContext().getCoreLibrary().getThreadBacktraceLocationClass();

            final Backtrace backtrace = RubyCallStack.getBacktrace(this, 1 + omit, true);

            int locationsCount = backtrace.getActivations().size();

            if (length != -1 && locationsCount > length) {
                locationsCount = length;
            }

            final Object[] locations = new Object[locationsCount];

            for (int n = 0; n < locationsCount; n++) {
                Activation activation = backtrace.getActivations().get(n);
                locations[n] = ThreadBacktraceLocationNodes.createRubyThreadBacktraceLocation(threadBacktraceLocationClass, activation);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), locations, locations.length);
        }
    }

    @CoreMethod(names = "class")
    public abstract static class KernelClassNode extends CoreMethodArrayArgumentsNode {

        @Child private ClassNode classNode;

        public KernelClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyClass getClass(VirtualFrame frame, Object self) {
            return classNode.executeGetClass(frame, self);
        }

    }

    public abstract static class CopyNode extends UnaryCoreMethodNode {

        public CopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract RubyBasicObject executeCopy(VirtualFrame frame, RubyBasicObject self);

        @Specialization
        public RubyBasicObject copy(VirtualFrame frame, RubyBasicObject self) {
            // This method is pretty crappy for compilation - it should improve with the OM

            final RubyBasicObject newObject = self.getLogicalClass().allocate(this);

            newObject.getObjectType().setInstanceVariables(newObject, self.getObjectType().getInstanceVariables(self));

            return newObject;
        }

    }

    @CoreMethod(names = "clone", taintFromSelf = true)
    public abstract static class CloneNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile frozenProfile = ConditionProfile.createBinaryProfile();

        @Child private CopyNode copyNode;
        @Child private CallDispatchHeadNode initializeCloneNode;
        @Child private IsFrozenNode isFrozenNode;
        @Child private FreezeNode freezeNode;
        @Child private SingletonClassNode singletonClassNode;

        public CloneNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            copyNode = CopyNodeFactory.create(context, sourceSection, null);
            // Calls private initialize_clone on the new copy.
            initializeCloneNode = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.CALL_METHOD_MISSING);
            isFrozenNode = IsFrozenNodeGen.create(context, sourceSection, null);
            freezeNode = FreezeNodeGen.create(context, sourceSection, null);
            singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyBasicObject clone(VirtualFrame frame, RubyBasicObject self) {
            CompilerDirectives.transferToInterpreter();

            final RubyBasicObject newObject = copyNode.executeCopy(frame, self);

            // Copy the singleton class if any.
            if (self.getMetaClass().isSingleton()) {
                singletonClassNode.executeSingletonClass(frame, newObject).initCopy(self.getMetaClass());
            }

            initializeCloneNode.call(frame, newObject, "initialize_clone", null, self);

            if (frozenProfile.profile(isFrozenNode.executeIsFrozen(self))) {
                freezeNode.executeFreeze(newObject);
            }

            return newObject;
        }

    }

    @CoreMethod(names = "dup", taintFromSelf = true)
    public abstract static class DupNode extends CoreMethodArrayArgumentsNode {

        @Child private CopyNode copyNode;
        @Child private CallDispatchHeadNode initializeDupNode;

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            copyNode = CopyNodeFactory.create(context, sourceSection, null);
            // Calls private initialize_dup on the new copy.
            initializeDupNode = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.CALL_METHOD_MISSING);
        }

        @Specialization
        public RubyBasicObject dup(VirtualFrame frame, RubyBasicObject self) {
            final RubyBasicObject newObject = copyNode.executeCopy(frame, self);

            initializeDupNode.call(frame, newObject, "initialize_dup", null, self);

            return newObject;
        }

    }

    @CoreMethod(names = "eval", isModuleFunction = true, required = 1, optional = 3, lowerFixnumParameters = 3)
    @NodeChildren({
            @NodeChild(value = "source", type = RubyNode.class),
            @NodeChild(value = "binding", type = RubyNode.class),
            @NodeChild(value = "filename", type = RubyNode.class),
            @NodeChild(value = "lineNumber", type = RubyNode.class)
    })
    public abstract static class EvalNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode toStr;
        @Child private BindingNode bindingNode;

        public EvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("source") public RubyNode coerceSourceToString(RubyNode source) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), source);
        }

        protected RubyBinding getCallerBinding(VirtualFrame frame) {
            if (bindingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                bindingNode = insert(KernelNodesFactory.BindingNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
            }
            return bindingNode.executeRubyBinding(frame);
        }

        @Specialization
        public Object eval(VirtualFrame frame, RubyString source, UndefinedPlaceholder binding, UndefinedPlaceholder filename, UndefinedPlaceholder lineNumber) {
            CompilerDirectives.transferToInterpreter();

            return getContext().eval(source.getByteList(), getCallerBinding(frame), true, this);
        }

        @Specialization(guards = "isNil(noBinding)")
        public Object eval(VirtualFrame frame, RubyString source, Object noBinding, RubyString filename, int lineNumber) {
            CompilerDirectives.transferToInterpreter();

            // TODO (nirvdrum Dec. 29, 2014) Do something with the supplied filename.
            return eval(frame, source, UndefinedPlaceholder.INSTANCE, UndefinedPlaceholder.INSTANCE, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public Object eval(RubyString source, RubyBinding binding, UndefinedPlaceholder filename, UndefinedPlaceholder lineNumber) {
            CompilerDirectives.transferToInterpreter();

            return getContext().eval(source.getByteList(), binding, false, this);
        }

        @Specialization
        public Object eval(RubyString source, RubyBinding binding, RubyString filename, UndefinedPlaceholder lineNumber) {
            CompilerDirectives.transferToInterpreter();

            return getContext().eval(source.getByteList(), binding, false, filename.toString(), this);
        }

        @Specialization
        public Object eval(RubyString source, RubyBinding binding, RubyString filename, int lineNumber) {
            CompilerDirectives.transferToInterpreter();

            return getContext().eval(source.getByteList(), binding, false, filename.toString(), this);
        }

        @Specialization(guards = "!isRubyBinding(badBinding)")
        public Object eval(RubyString source, RubyBasicObject badBinding, UndefinedPlaceholder filename, UndefinedPlaceholder lineNumber) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorWrongArgumentType(badBinding, "binding", this));
        }
    }

    @CoreMethod(names = "exec", isModuleFunction = true, required = 1, argumentsAsArray = true)
    public abstract static class ExecNode extends CoreMethodArrayArgumentsNode {

        public ExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object require(Object[] args) {
            CompilerDirectives.transferToInterpreter();

            final String[] commandLine = new String[args.length];

            for (int n = 0; n < args.length; n++) {
                commandLine[n] = args[n].toString();
            }

            exec(getContext(), commandLine);

            return null;
        }

        @TruffleBoundary
        private static void exec(RubyContext context, String[] commandLine) {
            final ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.inheritIO();

            final RubyHash env = context.getCoreLibrary().getENV();

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(env)) {
                builder.environment().put(keyValue.getKey().toString(), keyValue.getValue().toString());
            }

            final Process process;

            try {
                process = builder.start();
            } catch (IOException e) {
                // TODO(cs): proper Ruby exception
                throw new RuntimeException(e);
            }

            int exitCode = context.getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Integer>() {
                @Override
                public Integer block() throws InterruptedException {
                    return process.waitFor();
                }
            });

            System.exit(exitCode);
        }

    }

    @CoreMethod(names = "exit", isModuleFunction = true, optional = 1, lowerFixnumParameters = 0)
    public abstract static class ExitNode extends CoreMethodArrayArgumentsNode {

        public ExitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object exit(UndefinedPlaceholder exitCode) {
            return exit(0);
        }

        @Specialization
        public Object exit(int exitCode) {
            CompilerDirectives.transferToInterpreter();

            getContext().shutdown();
            System.exit(exitCode);
            return null;
        }

        @Specialization
        public Object exit(boolean status) {
            CompilerDirectives.transferToInterpreter();

            getContext().shutdown();
            System.exit(status ? 0 : -1);
            return null;
        }

    }

    @CoreMethod(names = "exit!", isModuleFunction = true, optional = 1)
    public abstract static class ExitBangNode extends CoreMethodArrayArgumentsNode {

        public ExitBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject exit(UndefinedPlaceholder exitCode) {
            return exit(1);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject exit(int exitCode) {
            getContext().innerShutdown(false);
            throw new MainExitException(exitCode, true);
        }

    }

    @CoreMethod(names = "fork", isModuleFunction = true, argumentsAsArray = true)
    public abstract static class ForkNode extends CoreMethodArrayArgumentsNode {

        public ForkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object fork(Object[] args) {
            CompilerDirectives.transferToInterpreter();
            getContext().getWarnings().warn("Kernel#fork not implemented - defined to satisfy some metaprogramming in RubySpec");
            return nil();
        }

    }

    @CoreMethod(names = "freeze")
    public abstract static class KernelFreezeNode extends CoreMethodArrayArgumentsNode {

        @Child private FreezeNode freezeNode;

        public KernelFreezeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object freeze(Object self) {
            if (freezeNode == null) {
                CompilerDirectives.transferToInterpreter();
                freezeNode = insert(FreezeNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
            }

            return freezeNode.executeFreeze(self);
        }

    }

    @CoreMethod(names = "frozen?")
    public abstract static class KernelFrozenNode extends CoreMethodArrayArgumentsNode {

        @Child private IsFrozenNode isFrozenNode;

        public KernelFrozenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isFrozen(Object self) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreter();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
            }

            return isFrozenNode.executeIsFrozen(self);
        }

    }

    @CoreMethod(names = "gets", isModuleFunction = true)
    public abstract static class GetsNode extends CoreMethodArrayArgumentsNode {

        public GetsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString gets(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS): having some trouble interacting with JRuby stdin - so using this hack
            final InputStream in = getContext().getRuntime().getInstanceConfig().getInput();

            Encoding encoding = getContext().getRuntime().getDefaultExternalEncoding();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding.getCharset()));

            final String line = getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<String>() {
                @Override
                public String block() throws InterruptedException {
                    return gets(reader);
                }
            });

            final RubyString rubyLine = getContext().makeString(line);

            // Set the local variable $_ in the caller

            final Frame caller = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

            final FrameSlot slot = caller.getFrameDescriptor().findFrameSlot("$_");

            if (slot != null) {
                caller.setObject(slot, WrapInThreadLocalNode.wrap(getContext(), rubyLine));
            }

            return rubyLine;
        }

        @TruffleBoundary
        private static String gets(BufferedReader reader) throws InterruptedException {
            try {
                return reader.readLine() + "\n";
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(int value) {
            // TODO(CS): should check this matches MRI
            return value;
        }

        @Specialization
        public int hash(long value) {
            // TODO(CS): should check this matches MRI
            return Long.valueOf(value).hashCode();
        }

        @Specialization
        public int hash(double value) {
            // TODO(CS): should check this matches MRI
            return Double.valueOf(value).hashCode();
        }

        @Specialization
        public int hash(boolean value) {
            return Boolean.valueOf(value).hashCode();
        }

        @Specialization
        public int hash(RubyBasicObject self) {
            // TODO(CS 8 Jan 15) we shouldn't use the Java class hierarchy like this - every class should define it's
            // own @CoreMethod hash
            return System.identityHashCode(self);
        }

    }

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object initializeCopy(RubyBasicObject self, RubyBasicObject from) {
            CompilerDirectives.transferToInterpreter();

            if (self.getLogicalClass() != from.getLogicalClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("initialize_copy should take same class object", this));
            }

            return self;
        }

    }

    @CoreMethod(names = { "initialize_dup", "initialize_clone" }, required = 1)
    public abstract static class InitializeDupCloneNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode initializeCopyNode;

        public InitializeDupCloneNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initializeCopyNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        @Specialization
        public Object initializeDup(VirtualFrame frame, RubyBasicObject self, RubyBasicObject from) {
            return initializeCopyNode.call(frame, self, "initialize_copy", null, from);
        }

    }

    @CoreMethod(names = "instance_of?", required = 1)
    public abstract static class InstanceOfNode extends CoreMethodArrayArgumentsNode {

        @Child private ClassNode classNode;

        public InstanceOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public boolean instanceOf(VirtualFrame frame, Object self, RubyClass rubyClass) {
            return classNode.executeGetClass(frame, self) == rubyClass;
        }

    }

    @CoreMethod(names = "instance_variable_defined?", required = 1)
    public abstract static class InstanceVariableDefinedNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubyString name) {
            CompilerDirectives.transferToInterpreter();

            return object.isFieldDefined(RubyContext.checkInstanceVariableName(getContext(), name.toString(), this));
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubySymbol name) {
            CompilerDirectives.transferToInterpreter();

            return object.isFieldDefined(RubyContext.checkInstanceVariableName(getContext(), name.toString(), this));
        }

    }

    @CoreMethod(names = "instance_variable_get", required = 1)
    public abstract static class InstanceVariableGetNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object instanceVariableGet(RubyBasicObject object, RubyString name) {
            return instanceVariableGet(object, name.toString());
        }

        @TruffleBoundary
        @Specialization
        public Object instanceVariableGet(RubyBasicObject object, RubySymbol name) {
            return instanceVariableGet(object, name.toString());
        }

        private Object instanceVariableGet(RubyBasicObject object, String name) {
            return object.getInstanceVariable(RubyContext.checkInstanceVariableName(getContext(), name, this));
        }

    }

    @CoreMethod(names = { "instance_variable_set", "__instance_variable_set__" }, raiseIfFrozenSelf = true, required = 2)
    public abstract static class InstanceVariableSetNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        // TODO CS 4-Mar-15 this badly needs to be cached

        @TruffleBoundary
        @Specialization
        public Object instanceVariableSet(RubyBasicObject object, RubyString name, Object value) {
            object.getObjectType().setInstanceVariable(object, RubyContext.checkInstanceVariableName(getContext(), name.toString(), this), value);
            return value;
        }

        @TruffleBoundary
        @Specialization
        public Object instanceVariableSet(RubyBasicObject object, RubySymbol name, Object value) {
            object.getObjectType().setInstanceVariable(object, RubyContext.checkInstanceVariableName(getContext(), name.toString(), this), value);
            return value;
        }

    }

    @CoreMethod(names = {"instance_variables", "__instance_variables__"})
    public abstract static class InstanceVariablesNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray instanceVariables(RubyBasicObject self) {
            CompilerDirectives.transferToInterpreter();

            final Object[] instanceVariableNames = self.getObjectType().getFieldNames(self);

            Arrays.sort(instanceVariableNames);

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (Object name : instanceVariableNames) {
                if (name instanceof String) {
                    array.slowPush(getContext().getSymbolTable().getSymbol((String) name));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = {"is_a?", "kind_of?"}, required = 1)
    public abstract static class IsANode extends CoreMethodArrayArgumentsNode {

        public IsANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract boolean executeIsA(VirtualFrame frame, Object self, RubyModule rubyClass);

        @Specialization(guards = {"isNil(nil)", "!isRubyModule(nil)"})
        public boolean isANil(RubyBasicObject self, Object nil) {
            return false;
        }

        @TruffleBoundary
        @Specialization
        public boolean isA(Object self, RubyModule rubyClass) {
            CompilerDirectives.transferToInterpreter();
            // TODO(CS): fast path
            return ModuleOperations.assignableTo(getContext().getCoreLibrary().getMetaClass(self), rubyClass);
        }

    }

    @CoreMethod(names = "lambda", isModuleFunction = true, needsBlock = true)
    public abstract static class LambdaNode extends CoreMethodArrayArgumentsNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyProc proc(RubyProc block) {
            return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.LAMBDA,
                    block.getSharedMethodInfo(), block.getCallTargetForMethods(), block.getCallTargetForMethods(),
                    block.getCallTargetForMethods(), block.getDeclarationFrame(), block.getMethod(),
                    block.getSelfCapturedInScope(), block.getBlockCapturedInScope());
        }
    }

    @CoreMethod(names = "load", isModuleFunction = true, required = 1)
    public abstract static class LoadNode extends CoreMethodArrayArgumentsNode {

        public LoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean load(RubyString file) {
            CompilerDirectives.transferToInterpreter();

            try {
                getContext().loadFile(file.toString(), this);
            } catch (RuntimeException e) {
                // TODO (nirvdrum 05-Feb-15) This is ugly, but checked exceptions are wrapped up the call stack. We need to revisit this.
                if (e.getCause() instanceof java.io.IOException) {
                    CompilerDirectives.transferToInterpreter();

                    throw new RaiseException(getContext().getCoreLibrary().loadErrorCannotLoad(file.toString(), this));
                } else {
                    throw e;
                }
            }

            return true;
        }
    }

    @CoreMethod(names = "local_variables", needsSelf = false)
    public abstract static class LocalVariablesNode extends CoreMethodArrayArgumentsNode {

        public LocalVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray localVariables() {
            CompilerDirectives.transferToInterpreter();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (Object name : Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_ONLY, false).getFrameDescriptor().getIdentifiers()) {
                if (name instanceof String) {
                    array.slowPush(getContext().getSymbol((String) name));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "__method__", needsSelf = false)
    public abstract static class MethodNameNode extends CoreMethodArrayArgumentsNode {

        public MethodNameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol methodName(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();
            // the "original/definition name" of the method.
            return getContext().getSymbolTable().getSymbol(RubyCallStack.getCallingMethod(frame).getSharedMethodInfo().getName());
        }

    }

    @CoreMethod(names = "method", required = 1)
    public abstract static class MethodNode extends CoreMethodArrayArgumentsNode {

        public MethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyMethod method(Object object, RubySymbol name) {
            return method(object, name.toString());
        }

        @Specialization
        public RubyMethod method(Object object, RubyString name) {
            return method(object, name.toString());
        }

        private RubyMethod method(Object object, String name) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS, 11-Jan-15) cache this lookup

            final InternalMethod method = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(object), name);

            if (method == null) {
                throw new RaiseException(
                    getContext().getCoreLibrary().nameErrorUndefinedMethod(
                        name, getContext().getCoreLibrary().getLogicalClass(object), this));
            }

            return new RubyMethod(getContext().getCoreLibrary().getMethodClass(), object, method);
        }

    }

    @CoreMethod(names = "methods", optional = 1)
    public abstract static class MethodsNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        @Child private SingletonMethodsNode singletonMethodsNode;
        @Child private BooleanCastNode booleanCastNode;

        public MethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyArray methods(VirtualFrame frame, Object self, UndefinedPlaceholder regular) {
            return methods(frame, self, true);
        }

        @Specialization
        public RubyArray methods(VirtualFrame frame, Object self, boolean regular) {
            RubyClass metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            if (regular) {
                return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        metaClass.filterMethodsOnObject(regular, MethodFilter.PUBLIC_PROTECTED).toArray());
            } else {
                if (singletonMethodsNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    singletonMethodsNode = insert(SingletonMethodsNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] { null }));
                }
                return singletonMethodsNode.executeSingletonMethods(frame, self, false);
            }
        }

        @Specialization
        public RubyArray methods(VirtualFrame frame, RubyBasicObject self, Object regular) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return methods(frame, self, booleanCastNode.executeBoolean(frame, regular));
        }

    }

    @CoreMethod(names = "nil?", needsSelf = false)
    public abstract static class NilNode extends CoreMethodArrayArgumentsNode {

        public NilNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isNil() {
            return false;
        }
    }

    @CoreMethod(names = "private_methods", optional = 1)
    public abstract static class PrivateMethodsNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        @Child private BooleanCastNode booleanCastNode;

        public PrivateMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyArray privateMethods(VirtualFrame frame, Object self, UndefinedPlaceholder includeAncestors) {
            return privateMethods(frame, self, true);
        }

        @Specialization
        public RubyArray privateMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            RubyClass metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    metaClass.filterMethodsOnObject(includeAncestors, MethodFilter.PRIVATE).toArray());
        }

        @Specialization
        public RubyArray privateMethods(VirtualFrame frame, RubyBasicObject self, Object includeAncestors) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return privateMethods(frame, self, booleanCastNode.executeBoolean(frame, includeAncestors));
        }

    }

    @CoreMethod(names = "proc", isModuleFunction = true, needsBlock = true)
    public abstract static class ProcNode extends CoreMethodArrayArgumentsNode {

        public ProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyProc proc(RubyProc block) {
            CompilerDirectives.transferToInterpreter();

            return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
                    block.getSharedMethodInfo(), block.getCallTargetForProcs(), block.getCallTargetForProcs(),
                    block.getCallTargetForMethods(), block.getDeclarationFrame(), block.getMethod(),
                    block.getSelfCapturedInScope(), block.getBlockCapturedInScope());
        }
    }

    @CoreMethod(names = "protected_methods", optional = 1)
    public abstract static class ProtectedMethodsNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        @Child private BooleanCastNode booleanCastNode;

        public ProtectedMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyArray protectedMethods(VirtualFrame frame, Object self, UndefinedPlaceholder includeAncestors) {
            return protectedMethods(frame, self, true);
        }

        @Specialization
        public RubyArray protectedMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            RubyClass metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    metaClass.filterMethodsOnObject(includeAncestors, MethodFilter.PROTECTED).toArray());
        }

        @Specialization
        public RubyArray protectedMethods(VirtualFrame frame, RubyBasicObject self, Object includeAncestors) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return protectedMethods(frame, self, booleanCastNode.executeBoolean(frame, includeAncestors));
        }

    }

    @CoreMethod(names = "public_methods", optional = 1)
    public abstract static class PublicMethodsNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        @Child private BooleanCastNode booleanCastNode;

        public PublicMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyArray publicMethods(VirtualFrame frame, Object self, UndefinedPlaceholder includeAncestors) {
            return publicMethods(frame, self, true);
        }

        @Specialization
        public RubyArray publicMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            RubyClass metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    metaClass.filterMethodsOnObject(includeAncestors, MethodFilter.PUBLIC).toArray());
        }

        @Specialization
        public RubyArray publicMethods(VirtualFrame frame, RubyBasicObject self, Object includeAncestors) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return publicMethods(frame, self, booleanCastNode.executeBoolean(frame, includeAncestors));
        }

    }

    @CoreMethod(names = "rand", isModuleFunction = true, optional = 1)
    public abstract static class RandNode extends CoreMethodArrayArgumentsNode {

        public RandNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double rand(UndefinedPlaceholder undefined) {
            return getContext().getRandom().nextDouble();
        }

        @Specialization(guards = "isZero(max)")
        public double randZero(int max) {
            return getContext().getRandom().nextDouble();
        }

        @Specialization(guards = "isNonZero(max)")
        public int randNonZero(int max) {
            return getContext().getRandom().nextInt(max);
        }

        @Specialization(guards = "isZero(max)")
        public double randZero(long max) {
            return getContext().getRandom().nextDouble();
        }

        @Specialization(guards = "isNonZero(max)")
        public long randNonZero(long max) {
            return getContext().getRandom().nextLong() % max;
        }

        protected boolean isZero(int max) {
            return max == 0;
        }

        protected boolean isNonZero(int max) {
            return max != 0;
        }

        protected boolean isZero(long max) {
            return max == 0;
        }

        protected boolean isNonZero(long max) {
            return max != 0;
        }

    }

    @CoreMethod(names = "require", isModuleFunction = true, required = 1)
    public abstract static class RequireNode extends CoreMethodArrayArgumentsNode {

        public RequireNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean require(RubyString feature) {
            CompilerDirectives.transferToInterpreter();

            // TODO CS 1-Mar-15 ERB will use strscan if it's there, but strscan is not yet complete, so we need to hide it

            if (feature.toString().equals("strscan") && Truffle.getRuntime().getCallerFrame().getCallNode()
                    .getEncapsulatingSourceSection().getSource().getName().endsWith("erb.rb")) {
                throw new RaiseException(getContext().getCoreLibrary().loadErrorCannotLoad(feature.toString(), this));
            }

            try {
                getContext().getFeatureManager().require(feature.toString(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return true;
        }
    }

    @CoreMethod(names = "require_relative", isModuleFunction = true, required = 1)
    public abstract static class RequireRelativeNode extends CoreMethodArrayArgumentsNode {

        public RequireRelativeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean requireRelative(RubyString feature) {
            CompilerDirectives.transferToInterpreter();

            final FeatureManager featureManager = getContext().getFeatureManager();

            final String featureString = feature.toString();
            final String featurePath;

            if (featureManager.isAbsolutePath(featureString)) {
                featurePath = featureString;
            } else {
                final Source source = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource();
                final String sourcePath = featureManager.getSourcePath(source);

                if (sourcePath == null) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().loadError("cannot infer basepath", this));
                }

                featurePath = dirname(sourcePath) + "/" + featureString;
            }

            try {
                featureManager.require(featurePath, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return true;
        }

        private String dirname(String path) {
            int lastSlash = path.lastIndexOf('/');
            assert lastSlash > 0;
            return path.substring(0, lastSlash);
        }
    }

    @CoreMethod(names = "respond_to?", required = 1, optional = 1)
    public abstract static class RespondToNode extends CoreMethodArrayArgumentsNode {

        @Child private DoesRespondDispatchHeadNode dispatch;
        @Child private DoesRespondDispatchHeadNode dispatchIgnoreVisibility;

        public RespondToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatch = new DoesRespondDispatchHeadNode(context, false, false, MissingBehavior.RETURN_MISSING, null);
            dispatchIgnoreVisibility = new DoesRespondDispatchHeadNode(context, true, false, MissingBehavior.RETURN_MISSING, null);

            if (DispatchNode.DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                dispatch.forceUncached();
                dispatchIgnoreVisibility.forceUncached();
            }
        }

        public abstract boolean executeDoesRespondTo(VirtualFrame frame, Object object, Object name, boolean includePrivate);

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubyString name, UndefinedPlaceholder checkVisibility) {
            return doesRespondTo(frame, object, name, false);
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubyString name, RubyBasicObject checkVisibility) {
            return doesRespondTo(frame, object, name, false);
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubyString name, boolean ignoreVisibility) {
            if (ignoreVisibility) {
                return dispatchIgnoreVisibility.doesRespondTo(frame, name, object);
            } else {
                return dispatch.doesRespondTo(frame, name, object);
            }
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubySymbol name, UndefinedPlaceholder checkVisibility) {
            return doesRespondTo(frame, object, name, false);
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubySymbol name, RubyBasicObject checkVisibility) {
            return doesRespondTo(frame, object, name, false);
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubySymbol name, boolean ignoreVisibility) {
            if (ignoreVisibility) {
                return dispatchIgnoreVisibility.doesRespondTo(frame, name, object);
            } else {
                return dispatch.doesRespondTo(frame, name, object);
            }
        }

    }

    @CoreMethod(names = "respond_to_missing?", required = 1, optional = 1)
    public abstract static class RespondToMissingNode extends CoreMethodArrayArgumentsNode {

        public RespondToMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubyString name, UndefinedPlaceholder includeAll) {
            return false;
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubySymbol name, UndefinedPlaceholder includeAll) {
            return false;
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubySymbol name, boolean includeAll) {
            return false;
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubyString name, boolean includeAll) {
            return false;
        }

    }

    @CoreMethod(names = "set_trace_func", isModuleFunction = true, required = 1)
    public abstract static class SetTraceFuncNode extends CoreMethodArrayArgumentsNode {

        public SetTraceFuncNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNil(nil)")
        public RubyBasicObject setTraceFunc(Object nil) {
            CompilerDirectives.transferToInterpreter();

            getContext().getTraceManager().setTraceFunc(null);
            return nil();
        }

        @Specialization
        public RubyProc setTraceFunc(RubyProc traceFunc) {
            CompilerDirectives.transferToInterpreter();

            getContext().getTraceManager().setTraceFunc(traceFunc);
            return traceFunc;
        }
    }

    @CoreMethod(names = "singleton_class")
    public abstract static class SingletonClassMethodNode extends CoreMethodArrayArgumentsNode {

        @Child private SingletonClassNode singletonClassNode;

        public SingletonClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyClass singletonClass(VirtualFrame frame, Object self) {
            return singletonClassNode.executeSingletonClass(frame, self);
        }

    }

    @CoreMethod(names = "singleton_methods", optional = 1)
    public abstract static class SingletonMethodsNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        @Child private BooleanCastNode booleanCastNode;

        public SingletonMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        public abstract RubyArray executeSingletonMethods(VirtualFrame frame, Object self, boolean includeAncestors);

        @Specialization
        public RubyArray singletonMethods(VirtualFrame frame, Object self, UndefinedPlaceholder includeAncestors) {
            return singletonMethods(frame, self, true);
        }

        @Specialization
        public RubyArray singletonMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            RubyClass metaClass = metaClassNode.executeMetaClass(frame, self);

            if (!metaClass.isSingleton()) {
                return new RubyArray(getContext().getCoreLibrary().getArrayClass());
            }

            CompilerDirectives.transferToInterpreter();
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    metaClass.filterSingletonMethods(includeAncestors, MethodFilter.PUBLIC_PROTECTED).toArray());
        }

        @Specialization
        public RubyArray singletonMethods(VirtualFrame frame, Object self, Object includeAncestors) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return singletonMethods(frame, self, booleanCastNode.executeBoolean(frame, includeAncestors));
        }

    }

    @CoreMethod(names = "String", isModuleFunction = true, required = 1)
    public abstract static class StringNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toS;

        public StringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public RubyString string(RubyString value) {
            return value;
        }

        @Specialization(guards = "!isRubyString(value)")
        public Object string(VirtualFrame frame, Object value) {
            return toS.call(frame, value, "to_s", null);
        }

    }

    @CoreMethod(names = "sleep", isModuleFunction = true, optional = 1)
    public abstract static class SleepNode extends CoreMethodArrayArgumentsNode {

        @Child NumericToFloatNode floatCastNode;

        public SleepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long sleep(UndefinedPlaceholder duration) {
            // TODO: this should actually be "forever".
            return doSleepMillis(Long.MAX_VALUE);
        }

        @Specialization
        public long sleep(int duration) {
            return doSleepMillis(duration * 1000L);
        }

        @Specialization
        public long sleep(long duration) {
            return doSleepMillis(duration * 1000);
        }

        @Specialization
        public long sleep(double duration) {
            return doSleepMillis((long) (duration * 1000));
        }

        @Specialization(guards = "isRubiniusUndefined(duration)")
        public long sleep(RubyBasicObject duration) {
            return sleep(UndefinedPlaceholder.INSTANCE);
        }

        @Specialization(guards = "!isRubiniusUndefined(duration)")
        public long sleep(VirtualFrame frame, RubyBasicObject duration) {
            if (floatCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                floatCastNode = insert(NumericToFloatNodeGen.create(getContext(), getSourceSection(), "to_f", null));
            }
            return sleep(floatCastNode.executeDouble(frame, duration));
        }

        @TruffleBoundary
        private long doSleepMillis(final long durationInMillis) {
            if (durationInMillis < 0) {
                throw new RaiseException(getContext().getCoreLibrary().argumentError("time interval must be positive", this));
            }

            final long start = System.currentTimeMillis();
            final RubyThread thread = getContext().getThreadManager().getCurrentThread();

            long slept = getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Long>() {
                boolean shouldWakeUp = false;

                @Override
                public Long block() throws InterruptedException {
                    long now = System.currentTimeMillis();
                    long slept = now - start;

                    if (shouldWakeUp || slept >= durationInMillis) {
                        return slept;
                    }

                    try {
                        Thread.sleep(durationInMillis - slept);
                        return System.currentTimeMillis() - start;
                    } catch (InterruptedException e) {
                        if (thread.getStatus() == Status.RUN) { // Thread#{wakeup,run}
                            shouldWakeUp = true;
                        }
                        throw e;
                    }
                }
            });

            return slept / 1000;
        }

    }

    @CoreMethod(names = {"format", "sprintf"}, isModuleFunction = true, argumentsAsArray = true, required = 1, taintFromParameter = 0)
    public abstract static class FormatNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode;

        public FormatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyString(firstArgument(arguments))", "byteListsEqual(asRubyString(firstArgument(arguments)), cachedFormat)"})
        public RubyString formatCached(
                VirtualFrame frame,
                Object[] arguments,
                @Cached("privatizeByteList(asRubyString(firstArgument(arguments)))") ByteList cachedFormat,
                @Cached("create(compileFormat(asRubyString(firstArgument(arguments))))") DirectCallNode callPackNode) {
            final Object[] store = ArrayUtils.extractRange(arguments, 1, arguments.length);

            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, new Object[]{store, store.length});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishFormat(cachedFormat, result);
        }

        @Specialization(guards = "isRubyString(firstArgument(arguments))", contains = "formatCached")
        public RubyString formatUncached(
                VirtualFrame frame,
                Object[] arguments,
                @Cached("create()") IndirectCallNode callPackNode) {
            final RubyString format = (RubyString) arguments[0];
            final Object[] store = ArrayUtils.extractRange(arguments, 1, arguments.length);

            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, compileFormat((RubyString) arguments[0]), new Object[]{store, store.length});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishFormat(format.getByteList(), result);
        }

        private RuntimeException handleException(PackException exception) {
            try {
                throw exception;
            } catch (TooFewArgumentsException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("too few arguments", this));
            } catch (NoImplicitConversionException e) {
                return new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(e.getObject(), e.getTarget(), this));
            } catch (OutsideOfStringException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("X outside of string", this));
            } catch (CantCompressNegativeException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("can't compress negative numbers", this));
            } catch (RangeException e) {
                return new RaiseException(getContext().getCoreLibrary().rangeError(e.getMessage(), this));
            } catch (CantConvertException e) {
                return new RaiseException(getContext().getCoreLibrary().typeError(e.getMessage(), this));
            }
        }

        private RubyString finishFormat(ByteList format, PackResult result) {
            final RubyString string = getContext().makeString(new ByteList(result.getOutput(), 0, result.getOutputLength()));

            if (format.length() == 0) {
                string.forceEncoding(USASCIIEncoding.INSTANCE);
            } else {
                switch (result.getEncoding()) {
                    case DEFAULT:
                    case ASCII_8BIT:
                        break;
                    case US_ASCII:
                        string.forceEncoding(USASCIIEncoding.INSTANCE);
                        break;
                    case UTF_8:
                        string.forceEncoding(UTF8Encoding.INSTANCE);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(TaintNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
                }

                taintNode.executeTaint(string);
            }

            return string;
        }

        protected ByteList privatizeByteList(RubyString string) {
            return string.getByteList().dup();
        }

        protected boolean byteListsEqual(RubyString string, ByteList byteList) {
            return string.getByteList().equal(byteList);
        }

        protected CallTarget compileFormat(RubyString format) {
            try {
                return new FormatParser(getContext()).parse(format.getByteList());
            } catch (FormatException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(e.getMessage(), this));
            }
        }

        protected Object firstArgument(Object[] args) {
            return args[0];
        }

        protected RubyString asRubyString(Object arg) {
            return (RubyString) arg;
        }

    }

    @CoreMethod(names = "system", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class SystemNode extends CoreMethodArrayArgumentsNode {

        public SystemNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean system(RubyString command) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS 5-JAN-15): very simplistic implementation

            final RubyHash env = getContext().getCoreLibrary().getENV();

            final List<String> envp = new ArrayList<>();

            // TODO(CS): cast
            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(env)) {
                envp.add(keyValue.getKey().toString() + "=" + keyValue.getValue().toString());
            }

            // We need to run via bash to get the variable and other expansion we expect
            try {
                Runtime.getRuntime().exec(new String[]{"bash", "-c", command.toString()}, envp.toArray(new String[envp.size()]));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return true;
        }

    }

    @CoreMethod(names = "taint")
    public abstract static class KernelTaintNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode;

        public KernelTaintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object taint(Object object) {
            if (taintNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintNode = insert(TaintNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
            }
            return taintNode.executeTaint(object);
        }

    }

    @CoreMethod(names = "tainted?")
    public abstract static class KernelIsTaintedNode extends CoreMethodArrayArgumentsNode {

        @Child private IsTaintedNode isTaintedNode;

        public KernelIsTaintedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isTainted(Object object) {
            if (isTaintedNode == null) {
                CompilerDirectives.transferToInterpreter();
                isTaintedNode = insert(IsTaintedNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
            }
            return isTaintedNode.executeIsTainted(object);
        }

    }

    public abstract static class ToHexStringNode extends CoreMethodArrayArgumentsNode {

        public ToHexStringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract String executeToHexString(VirtualFrame frame, Object value);

        @Specialization
        public String toHexString(int value) {
            return toHexString((long) value);
        }

        @Specialization
        public String toHexString(long value) {
            return Long.toHexString(value);
        }

        @Specialization(guards = "isRubyBignum(value)")
        public String toHexString(RubyBasicObject value) {
            return BignumNodes.getBigIntegerValue(value).toString(16);
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @Child private ClassNode classNode;
        @Child private ObjectPrimitiveNodes.ObjectIDPrimitiveNode objectIDNode;
        @Child private ToHexStringNode toHexStringNode;

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeGen.create(context, sourceSection, null);
            objectIDNode = ObjectPrimitiveNodesFactory.ObjectIDPrimitiveNodeFactory.create(context, sourceSection, new RubyNode[] { null });
            toHexStringNode = KernelNodesFactory.ToHexStringNodeFactory.create(context, sourceSection, new RubyNode[]{null});
        }

        public abstract RubyString executeToS(VirtualFrame frame, Object self);

        @Specialization
        public RubyString toS(VirtualFrame frame, Object self) {
            CompilerDirectives.transferToInterpreter();

            String className = classNode.executeGetClass(frame, self).getName();
            Object id = objectIDNode.executeObjectID(frame, self);
            String hexID = toHexStringNode.executeToHexString(frame, id);

            return getContext().makeString("#<" + className + ":0x" + hexID + ">");
        }

    }

    @CoreMethod(names = "untaint")
    public abstract static class UntaintNode extends CoreMethodArrayArgumentsNode {

        @Child private WriteHeadObjectFieldNode writeTaintNode;

        public UntaintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeTaintNode = new WriteHeadObjectFieldNode(RubyBasicObject.TAINTED_IDENTIFIER);
        }

        @Specialization
        public Object taint(boolean object) {
            return frozen(object);
        }

        @Specialization
        public Object taint(int object) {
            return frozen(object);
        }

        @Specialization
        public Object taint(long object) {
            return frozen(object);
        }

        @Specialization
        public Object taint(double object) {
            return frozen(object);
        }

        private Object frozen(Object object) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(getContext().getCoreLibrary().getLogicalClass(object).getName(), this));
        }


        @Specialization
        public Object taint(RubyBasicObject object) {
            writeTaintNode.execute(object, false);
            return object;
        }

    }

}
