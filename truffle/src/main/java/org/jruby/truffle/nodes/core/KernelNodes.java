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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jcodings.Encoding;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.ThreadLocalObjectNode;
import org.jruby.truffle.nodes.cast.NumericToFloatNode;
import org.jruby.truffle.nodes.cast.NumericToFloatNodeFactory;
import org.jruby.truffle.nodes.coerce.ToStrNodeFactory;
import org.jruby.truffle.nodes.control.WhileNode;
import org.jruby.truffle.nodes.core.KernelNodesFactory.SameOrEqualNodeFactory;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.globals.WrapInThreadLocalNode;
import org.jruby.truffle.nodes.literal.BooleanLiteralNode;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.nodes.rubinius.ObjectPrimitiveNodes;
import org.jruby.truffle.nodes.rubinius.ObjectPrimitiveNodesFactory;
import org.jruby.truffle.nodes.yield.YieldNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.backtrace.MRIBacktraceFormatter;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.FeatureManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

@CoreClass(name = "Kernel")
public abstract class KernelNodes {

    @CoreMethod(names = "`", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class BacktickNode extends CoreMethodNode {

        public BacktickNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BacktickNode(BacktickNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString backtick(RubyString command) {
            // Command is lexically a string interoplation, so variables will already have been expanded

            notDesignedForCompilation();

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
    public abstract static class SameOrEqualNode extends CoreMethodNode {

        @Child private BasicObjectNodes.ReferenceEqualNode referenceEqualNode;
        @Child private CallDispatchHeadNode equalNode;

        private final ConditionProfile sameProfile = ConditionProfile.createBinaryProfile();

        public SameOrEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SameOrEqualNode(SameOrEqualNode prev) {
            super(prev);
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
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass equal(Object other) {
            return nil();
        }

    }

    @CoreMethod(names = "!~", required = 1)
    public abstract static class NotMatchNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode matchNode;

        public NotMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            matchNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        public NotMatchNode(NotMatchNode prev) {
            super(prev);
            matchNode = prev.matchNode;
        }

        @Specialization
        public boolean notMatch(VirtualFrame frame, Object self, Object other) {
            return !matchNode.callBoolean(frame, self, "=~", null, other);
        }

    }

    @CoreMethod(names = {"<=>"}, required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        @Child private SameOrEqualNode equalNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
        }

        public CompareNode(CompareNode prev) {
            super(prev);
            equalNode = prev.equalNode;
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
    public abstract static class AbortNode extends CoreMethodNode {

        public AbortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbortNode(AbortNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass abort() {
            CompilerDirectives.transferToInterpreter();
            System.exit(1);
            return nil();
        }
    }

    @CoreMethod(names = "at_exit", isModuleFunction = true, needsBlock = true)
    public abstract static class AtExitNode extends CoreMethodNode {

        public AtExitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AtExitNode(AtExitNode prev) {
            super(prev);
        }

        @Specialization
        public Object atExit(RubyProc block) {
            notDesignedForCompilation();

            getContext().getAtExitManager().add(block);
            return nil();
        }
    }

    @CoreMethod(names = "binding", isModuleFunction = true)
    public abstract static class BindingNode extends CoreMethodNode {

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
    public abstract static class BlockGivenNode extends CoreMethodNode {

        public BlockGivenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BlockGivenNode(BlockGivenNode prev) {
            super(prev);
        }

        @Specialization
        public boolean blockGiven() {
            return RubyArguments.getBlock(Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_ONLY, false).getArguments()) != null;
        }
    }

    @CoreMethod(names = "__callee__", needsSelf = false)
    public abstract static class CalleeNameNode extends CoreMethodNode {

        public CalleeNameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CalleeNameNode(CalleeNameNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol calleeName(VirtualFrame frame) {
            notDesignedForCompilation();
            // the "called name" of a method.
            return getContext().getSymbolTable().getSymbol(RubyCallStack.getCallingMethod(frame).getName());
        }
    }

    @CoreMethod(names = "caller", isModuleFunction = true, optional = 1)
    public abstract static class CallerNode extends CoreMethodNode {

        public CallerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CallerNode(CallerNode prev) {
            super(prev);
        }

        @Specialization
        public Object caller(UndefinedPlaceholder omit) {
            return caller(1);
        }

        @Specialization
        public Object caller(int omit) {
            notDesignedForCompilation();

            omit += 1; // Always ignore this node

            Backtrace backtrace = RubyCallStack.getBacktrace(this);
            List<Activation> activations = backtrace.getActivations();
            int size = activations.size() - omit;

            if (size < 0) {
                return nil();
            }

            Object[] callers = new Object[size];
            for (int n = 0; n < size; n++) {
                callers[n] = getContext().makeString(MRIBacktraceFormatter.formatCallerLine(activations, n + omit));
            }
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), callers, callers.length);
        }
    }

    @CoreMethod(names = "class")
    public abstract static class KernelClassNode extends CoreMethodNode {

        @Child private ClassNode classNode;

        public KernelClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeFactory.create(context, sourceSection, null);
        }

        public KernelClassNode(KernelClassNode prev) {
            super(prev);
            classNode = prev.classNode;
        }

        @Specialization
        public RubyClass getClass(VirtualFrame frame, Object self) {
            return classNode.executeGetClass(frame, self);
        }

    }

    @CoreMethod(names = "clone", taintFromSelf = true)
    public abstract static class CloneNode extends CoreMethodNode {

        private final ConditionProfile frozenProfile = ConditionProfile.createBinaryProfile();

        @Child private CallDispatchHeadNode initializeCloneNode;
        @Child private IsFrozenNode isFrozenNode;
        @Child private FreezeNode freezeNode;

        public CloneNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            // Calls private initialize_clone on the new copy.
            initializeCloneNode = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.CALL_METHOD_MISSING);
            isFrozenNode = IsFrozenNodeFactory.create(context, sourceSection, null);
            freezeNode = FreezeNodeFactory.create(context, sourceSection, null);
        }

        public CloneNode(CloneNode prev) {
            super(prev);
            initializeCloneNode = prev.initializeCloneNode;
            isFrozenNode = prev.isFrozenNode;
            freezeNode = prev.freezeNode;
        }

        @Specialization
        public Object clone(VirtualFrame frame, RubyBasicObject self) {
            notDesignedForCompilation();

            final RubyBasicObject newObject = self.getLogicalClass().allocate(this);

            // Copy the singleton class if any.
            if (self.getMetaClass().isSingleton()) {
                newObject.getSingletonClass(this).initCopy(self.getMetaClass());
            }

            newObject.getOperations().setInstanceVariables(newObject, self.getOperations().getInstanceVariables(self));
            initializeCloneNode.call(frame, newObject, "initialize_clone", null, self);

            if (frozenProfile.profile(isFrozenNode.executeIsFrozen(self))) {
                freezeNode.executeFreeze(newObject);
            }

            return newObject;
        }

    }

    @CoreMethod(names = "dup", taintFromSelf = true)
    public abstract static class DupNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode initializeDupNode;

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            // Calls private initialize_dup on the new copy.
            initializeDupNode = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.CALL_METHOD_MISSING);
        }

        public DupNode(DupNode prev) {
            super(prev);
            initializeDupNode = prev.initializeDupNode;
        }

        @Specialization
        public Object dup(VirtualFrame frame, RubyBasicObject self) {
            // This method is pretty crappy for compilation - it should improve with the OM

            final RubyBasicObject newObject = self.getLogicalClass().allocate(this);
            newObject.getOperations().setInstanceVariables(newObject, self.getOperations().getInstanceVariables(self));
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
    public abstract static class EvalNode extends RubyNode {

        @Child private CallDispatchHeadNode toStr;
        @Child private BindingNode bindingNode;

        public EvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EvalNode(EvalNode prev) {
            super(prev);
        }

        @CreateCast("source") public RubyNode coerceSourceToString(RubyNode source) {
            return ToStrNodeFactory.create(getContext(), getSourceSection(), source);
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
            notDesignedForCompilation();

            return getContext().eval(source.getBytes(), getCallerBinding(frame), true, this);
        }

        @Specialization
        public Object eval(VirtualFrame frame, RubyString source, RubyNilClass noBinding, RubyString filename, int lineNumber) {
            notDesignedForCompilation();

            // TODO (nirvdrum Dec. 29, 2014) Do something with the supplied filename.
            return eval(frame, source, UndefinedPlaceholder.INSTANCE, UndefinedPlaceholder.INSTANCE, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public Object eval(RubyString source, RubyBinding binding, UndefinedPlaceholder filename, UndefinedPlaceholder lineNumber) {
            notDesignedForCompilation();

            return getContext().eval(source.getBytes(), binding, false, this);
        }

        @Specialization
        public Object eval(RubyString source, RubyBinding binding, RubyString filename, UndefinedPlaceholder lineNumber) {
            notDesignedForCompilation();

            return getContext().eval(source.getBytes(), binding, false, filename.toString(), this);
        }

        @Specialization
        public Object eval(RubyString source, RubyBinding binding, RubyString filename, int lineNumber) {
            notDesignedForCompilation();

            return getContext().eval(source.getBytes(), binding, false, filename.toString(), this);
        }

        @Specialization(guards = "!isRubyBinding(badBinding)")
        public Object eval(RubyString source, RubyBasicObject badBinding, UndefinedPlaceholder filename, UndefinedPlaceholder lineNumber) {
            throw new RaiseException(
                    getContext().getCoreLibrary().typeError(
                            String.format("wrong argument type %s (expected binding)",
                                    badBinding.getLogicalClass().getName()),
                            this));
        }
    }

    @CoreMethod(names = "exec", isModuleFunction = true, required = 1, argumentsAsArray = true)
    public abstract static class ExecNode extends CoreMethodNode {

        public ExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExecNode(ExecNode prev) {
            super(prev);
        }

        @Specialization
        public Object require(Object[] args) {
            notDesignedForCompilation();

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
    public abstract static class ExitNode extends CoreMethodNode {

        public ExitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitNode(ExitNode prev) {
            super(prev);
        }

        @Specialization
        public Object exit(UndefinedPlaceholder exitCode) {
            return exit(0);
        }

        @Specialization
        public Object exit(int exitCode) {
            notDesignedForCompilation();

            getContext().shutdown();
            System.exit(exitCode);
            return null;
        }

        @Specialization
        public Object exit(boolean status) {
            notDesignedForCompilation();

            getContext().shutdown();
            System.exit(status ? 0 : -1);
            return null;
        }

    }

    @CoreMethod(names = "exit!", isModuleFunction = true, optional = 1)
    public abstract static class ExitBangNode extends CoreMethodNode {

        public ExitBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitBangNode(ExitBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass exit(UndefinedPlaceholder exitCode) {
            return exit(1);
        }

        @Specialization
        public RubyNilClass exit(int exitCode) {
            CompilerDirectives.transferToInterpreter();
            System.exit(exitCode);
            return nil();
        }

    }

    @CoreMethod(names = "extend", argumentsAsArray = true, required = 1)
    public abstract static class ExtendNode extends CoreMethodNode {

        public ExtendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExtendNode(ExtendNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject extend(RubyBasicObject self, Object[] args) {
            notDesignedForCompilation();

            for (int n = 0; n < args.length; n++) {
                self.extend((RubyModule) args[n], this);
            }

            return self;
        }

    }

    @CoreMethod(names = "fork", isModuleFunction = true, argumentsAsArray = true)
    public abstract static class ForkNode extends CoreMethodNode {

        public ForkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ForkNode(ForkNode prev) {
            super(prev);
        }

        @Specialization
        public Object fork(Object[] args) {
            notDesignedForCompilation();
            getContext().getWarnings().warn("Kernel#fork not implemented - defined to satisfy some metaprogramming in RubySpec");
            return nil();
        }

    }

    @CoreMethod(names = "freeze")
    public abstract static class KernelFreezeNode extends CoreMethodNode {

        @Child private FreezeNode freezeNode;

        public KernelFreezeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KernelFreezeNode(KernelFreezeNode prev) {
            super(prev);
            freezeNode = prev.freezeNode;
        }

        @Specialization
        public Object freeze(Object self) {
            if (freezeNode == null) {
                CompilerDirectives.transferToInterpreter();
                freezeNode = insert(FreezeNodeFactory.create(getContext(), getEncapsulatingSourceSection(), null));
            }

            return freezeNode.executeFreeze(self);
        }

    }

    @CoreMethod(names = "frozen?")
    public abstract static class KernelFrozenNode extends CoreMethodNode {

        @Child private IsFrozenNode isFrozenNode;

        public KernelFrozenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KernelFrozenNode(KernelFrozenNode prev) {
            super(prev);
            isFrozenNode = prev.isFrozenNode;
        }

        @Specialization
        public boolean isFrozen(Object self) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreter();
                isFrozenNode = insert(IsFrozenNodeFactory.create(getContext(), getEncapsulatingSourceSection(), null));
            }

            return isFrozenNode.executeIsFrozen(self);
        }

    }

    @CoreMethod(names = "gets", isModuleFunction = true)
    public abstract static class GetsNode extends CoreMethodNode {

        public GetsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetsNode(GetsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString gets(VirtualFrame frame) {
            notDesignedForCompilation();

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
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HashNode(HashNode prev) {
            super(prev);
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

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeCopyNode(InitializeCopyNode prev) {
            super(prev);
        }

        @Specialization
        public Object initializeCopy(RubyBasicObject self, RubyBasicObject from) {
            notDesignedForCompilation();

            if (self.getLogicalClass() != from.getLogicalClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("initialize_copy should take same class object", this));
            }

            return self;
        }

    }

    @CoreMethod(names = {"initialize_dup", "initialize_clone"}, visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeDupCloneNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode initializeCopyNode;

        public InitializeDupCloneNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initializeCopyNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        public InitializeDupCloneNode(InitializeDupCloneNode prev) {
            super(prev);
            initializeCopyNode = prev.initializeCopyNode;
        }

        @Specialization
        public Object initializeDup(VirtualFrame frame, RubyBasicObject self, RubyBasicObject from) {
            return initializeCopyNode.call(frame, self, "initialize_copy", null, from);
        }

    }

    @CoreMethod(names = "instance_of?", required = 1)
    public abstract static class InstanceOfNode extends CoreMethodNode {

        @Child private ClassNode classNode;

        public InstanceOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeFactory.create(context, sourceSection, null);
        }

        public InstanceOfNode(InstanceOfNode prev) {
            super(prev);
            classNode = prev.classNode;
        }

        @Specialization
        public boolean instanceOf(VirtualFrame frame, Object self, RubyClass rubyClass) {
            return classNode.executeGetClass(frame, self) == rubyClass;
        }

    }

    @CoreMethod(names = "instance_variable_defined?", required = 1)
    public abstract static class InstanceVariableDefinedNode extends CoreMethodNode {

        public InstanceVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableDefinedNode(InstanceVariableDefinedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubyString name) {
            notDesignedForCompilation();

            return object.isFieldDefined(RubyContext.checkInstanceVariableName(getContext(), name.toString(), this));
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubySymbol name) {
            notDesignedForCompilation();

            return object.isFieldDefined(RubyContext.checkInstanceVariableName(getContext(), name.toString(), this));
        }

    }

    @CoreMethod(names = "instance_variable_get", required = 1)
    public abstract static class InstanceVariableGetNode extends CoreMethodNode {

        public InstanceVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableGetNode(InstanceVariableGetNode prev) {
            super(prev);
        }

        @Specialization
        public Object instanceVariableGet(RubyBasicObject object, RubyString name) {
            return instanceVariableGet(object, name.toString());
        }

        @Specialization
        public Object instanceVariableGet(RubyBasicObject object, RubySymbol name) {
            return instanceVariableGet(object, name.toString());
        }

        private Object instanceVariableGet(RubyBasicObject object, String name) {
            notDesignedForCompilation();

            return object.getInstanceVariable(RubyContext.checkInstanceVariableName(getContext(), name, this));
        }

    }

    @CoreMethod(names = { "instance_variable_set", "__instance_variable_set__" }, raiseIfFrozenSelf = true, required = 2)
    public abstract static class InstanceVariableSetNode extends CoreMethodNode {

        public InstanceVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableSetNode(InstanceVariableSetNode prev) {
            super(prev);
        }
        
        // TODO CS 4-Mar-15 this badly needs to be cached

        @TruffleBoundary
        @Specialization
        public Object instanceVariableSet(RubyBasicObject object, RubyString name, Object value) {
            object.getOperations().setInstanceVariable(object, RubyContext.checkInstanceVariableName(getContext(), name.toString(), this), value);
            return value;
        }

        @TruffleBoundary
        @Specialization
        public Object instanceVariableSet(RubyBasicObject object, RubySymbol name, Object value) {
            object.getOperations().setInstanceVariable(object, RubyContext.checkInstanceVariableName(getContext(), name.toString(), this), value);
            return value;
        }

    }

    @CoreMethod(names = {"instance_variables", "__instance_variables__"})
    public abstract static class InstanceVariablesNode extends CoreMethodNode {

        public InstanceVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariablesNode(InstanceVariablesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray instanceVariables(RubyBasicObject self) {
            notDesignedForCompilation();

            final Object[] instanceVariableNames = self.getOperations().getFieldNames(self);

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
    public abstract static class IsANode extends CoreMethodNode {

        public IsANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsANode(IsANode prev) {
            super(prev);
        }

        public abstract boolean executeIsA(VirtualFrame frame, Object self, RubyModule rubyClass);

        @Specialization
        public boolean isA(RubyBasicObject self, RubyNilClass nil) {
            return false;
        }

        @TruffleBoundary
        @Specialization
        public boolean isA(Object self, RubyModule rubyClass) {
            notDesignedForCompilation();
            // TODO(CS): fast path
            return ModuleOperations.assignableTo(getContext().getCoreLibrary().getMetaClass(self), rubyClass);
        }

    }

    @CoreMethod(names = "lambda", isModuleFunction = true, needsBlock = true)
    public abstract static class LambdaNode extends CoreMethodNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LambdaNode(LambdaNode prev) {
            super(prev);
        }

        @Specialization
        public RubyProc proc(RubyProc block) {
            notDesignedForCompilation();

            return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.LAMBDA,
                    block.getSharedMethodInfo(), block.getCallTargetForMethods(), block.getCallTargetForMethods(),
                    block.getCallTargetForMethods(), block.getDeclarationFrame(), block.getMethod(),
                    block.getSelfCapturedInScope(), block.getBlockCapturedInScope());
        }
    }

    @CoreMethod(names = "load", isModuleFunction = true, required = 1)
    public abstract static class LoadNode extends CoreMethodNode {

        public LoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LoadNode(LoadNode prev) {
            super(prev);
        }

        @Specialization
        public boolean load(RubyString file) {
            notDesignedForCompilation();

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
    public abstract static class LocalVariablesNode extends CoreMethodNode {

        public LocalVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LocalVariablesNode(LocalVariablesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray localVariables() {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (Object name : Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_ONLY, false).getFrameDescriptor().getIdentifiers()) {
                if (name instanceof String) {
                    array.slowPush(getContext().newSymbol((String) name));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "loop", isModuleFunction = true, returnsEnumeratorIfNoBlock = true)
    public abstract static class LoopNode extends CoreMethodNode {

        @Child private WhileNode whileNode;

        public LoopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            whileNode = WhileNode.createWhile(context, sourceSection,
                    new BooleanLiteralNode(context, sourceSection, true),
                    new YieldNode(context, getSourceSection(), new RubyNode[]{}, false)
            );
        }

        public LoopNode(LoopNode prev) {
            super(prev);
            whileNode = prev.whileNode;
        }

        @Specialization
        public Object loop(VirtualFrame frame) {
            return whileNode.execute(frame);
        }
    }

    @CoreMethod(names = "__method__", needsSelf = false)
    public abstract static class MethodNameNode extends CoreMethodNode {

        public MethodNameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodNameNode(MethodNameNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol methodName(VirtualFrame frame) {
            notDesignedForCompilation();
            // the "original/definition name" of the method.
            return getContext().getSymbolTable().getSymbol(RubyCallStack.getCallingMethod(frame).getSharedMethodInfo().getName());
        }

    }

    @CoreMethod(names = "method", required = 1)
    public abstract static class MethodNode extends CoreMethodNode {

        public MethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodNode(MethodNode prev) {
            super(prev);
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
            notDesignedForCompilation();

            // TODO(CS, 11-Jan-15) cache this lookup

            final InternalMethod method = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(object), name);

            if (method == null) {
                throw new UnsupportedOperationException();
            }

            return new RubyMethod(getContext().getCoreLibrary().getMethodClass(), object, method);
        }

    }

    @CoreMethod(names = "methods", optional = 1)
    public abstract static class MethodsNode extends CoreMethodNode {

        public MethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodsNode(MethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray methods(RubyBasicObject self, UndefinedPlaceholder unused) {
            return methods(self, true);
        }

        @Specialization
        public RubyArray methods(RubyBasicObject self, boolean includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getContext().getCoreLibrary().getArrayClass());

            Map<String, InternalMethod> methods;

            if (includeInherited) {
                methods = ModuleOperations.getAllMethods(self.getMetaClass());
            } else {
                methods = self.getMetaClass().getMethods();
            }

            for (InternalMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC || method.getVisibility() == Visibility.PROTECTED) {
                    array.slowPush(self.getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "nil?", needsSelf = false)
    public abstract static class NilNode extends CoreMethodNode {

        public NilNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NilNode(NilNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isNil() {
            return false;
        }
    }

    @CoreMethod(names = "private_methods", optional = 1)
    public abstract static class PrivateMethodsNode extends CoreMethodNode {

        public PrivateMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrivateMethodsNode(PrivateMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray private_methods(RubyBasicObject self, UndefinedPlaceholder unused) {
            return private_methods(self, true);
        }

        @Specialization
        public RubyArray private_methods(RubyBasicObject self, boolean includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getContext().getCoreLibrary().getArrayClass());

            Map<String, InternalMethod> methods;

            if (includeInherited) {
                methods = ModuleOperations.getAllMethods(self.getMetaClass());
            } else {
                methods = self.getMetaClass().getMethods();
            }

            for (InternalMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PRIVATE) {
                    array.slowPush(self.getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "proc", isModuleFunction = true, needsBlock = true)
    public abstract static class ProcNode extends CoreMethodNode {

        public ProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ProcNode(ProcNode prev) {
            super(prev);
        }

        @Specialization
        public RubyProc proc(RubyProc block) {
            notDesignedForCompilation();

            return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
                    block.getSharedMethodInfo(), block.getCallTargetForProcs(), block.getCallTargetForProcs(),
                    block.getCallTargetForMethods(), block.getDeclarationFrame(), block.getMethod(),
                    block.getSelfCapturedInScope(), block.getBlockCapturedInScope());
        }
    }

    @CoreMethod(names = "public_methods", optional = 1)
    public abstract static class PublicMethodsNode extends CoreMethodNode {

        public PublicMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicMethodsNode(PublicMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray methods(RubyBasicObject self, boolean includeInherited) {
            notDesignedForCompilation();

            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Object#methods always returns inherited methods at the moment");
            }

            return methods(self, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubyArray methods(RubyBasicObject self, UndefinedPlaceholder includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getContext().getCoreLibrary().getArrayClass());

            final Map<String, InternalMethod> methods = self.getMetaClass().getMethods();

            for (InternalMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC) {
                    array.slowPush(self.getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "raise", isModuleFunction = true, optional = 3)
    public abstract static class RaiseNode extends CoreMethodNode {

        @Child private ReadInstanceVariableNode getLastExceptionNode;
        @Child private CallDispatchHeadNode initialize;

        public RaiseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initialize = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public RaiseNode(RaiseNode prev) {
            super(prev);
            initialize = prev.initialize;
        }

        @Specialization
        public Object raise(VirtualFrame frame, UndefinedPlaceholder undefined1, UndefinedPlaceholder undefined2, UndefinedPlaceholder undefined3) {
            notDesignedForCompilation();

            if (getLastExceptionNode == null) {
                CompilerDirectives.transferToInterpreter();
                getLastExceptionNode = insert(new ReadInstanceVariableNode(getContext(), getSourceSection(), "$!",
                        new ThreadLocalObjectNode(getContext(), getSourceSection()),
                        true));
            }

            final Object lastException = getLastExceptionNode.execute(frame);

            if (lastException == nil()) {
                return raise(frame, getContext().makeString(""), UndefinedPlaceholder.INSTANCE, UndefinedPlaceholder.INSTANCE);
            }

            throw new RaiseException((RubyException) lastException);
        }

        @Specialization
        public Object raise(VirtualFrame frame, RubyString message, UndefinedPlaceholder undefined1, UndefinedPlaceholder undefined2) {
            notDesignedForCompilation();

            return raise(frame, getContext().getCoreLibrary().getRuntimeErrorClass(), message, undefined1);
        }

        @Specialization
        public Object raise(VirtualFrame frame, RubyClass exceptionClass, UndefinedPlaceholder undefined1, UndefinedPlaceholder undefined2) {
            notDesignedForCompilation();

            return raise(frame, exceptionClass, getContext().makeString(""), undefined1);
        }

        @Specialization
        public Object raise(VirtualFrame frame, RubyClass exceptionClass, RubyString message, UndefinedPlaceholder undefined1) {
            notDesignedForCompilation();

            final Object exception = exceptionClass.allocate(this);
            initialize.call(frame, exception, "initialize", null, message);

            if (!(exception instanceof RubyException)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("exception class/object expected", this));
            }

            throw new RaiseException((RubyException) exception);
        }

        // This provokes an error under standard Ruby:
        //   TypeError: backtrace must be Array of String
        // but is used in Rubinius in coerce_to_failed for instance.
        @Specialization
        public Object raise(VirtualFrame frame, RubyClass exceptionClass, RubyString message, RubyException backtrace) {
            // TODO (9 Mar. 2015): handle "backtrace" as an MRI "cause".
            return raise(frame, exceptionClass, message, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public Object raise(RubyException exception, UndefinedPlaceholder undefined1, UndefinedPlaceholder undefined2) {
            throw new RaiseException(exception);
        }

    }

    @CoreMethod(names = "rand", isModuleFunction = true, optional = 1)
    public abstract static class RandNode extends CoreMethodNode {

        public RandNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RandNode(RandNode prev) {
            super(prev);
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
    public abstract static class RequireNode extends CoreMethodNode {

        public RequireNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RequireNode(RequireNode prev) {
            super(prev);
        }

        @Specialization
        public boolean require(RubyString feature) {
            notDesignedForCompilation();

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
    public abstract static class RequireRelativeNode extends CoreMethodNode {

        public RequireRelativeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RequireRelativeNode(RequireRelativeNode prev) {
            super(prev);
        }

        @Specialization
        public boolean requireRelative(RubyString feature) {
            notDesignedForCompilation();

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
    public abstract static class RespondToNode extends CoreMethodNode {

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

        public RespondToNode(RespondToNode prev) {
            super(prev);
            dispatch = prev.dispatch;
            dispatchIgnoreVisibility = prev.dispatchIgnoreVisibility;
        }

        public abstract boolean executeDoesRespondTo(VirtualFrame frame, Object object, Object name, boolean includePrivate);

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubyString name, UndefinedPlaceholder checkVisibility) {
            return dispatch.doesRespondTo(frame, name, object);
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
            return dispatch.doesRespondTo(frame, name, object);
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

    @CoreMethod(names = "respond_to_missing?", required = 1, optional = 1, visibility = Visibility.PRIVATE)
    public abstract static class RespondToMissingNode extends CoreMethodNode {

        public RespondToMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RespondToMissingNode(RespondToMissingNode prev) {
            super(prev);
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
    public abstract static class SetTraceFuncNode extends CoreMethodNode {

        public SetTraceFuncNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetTraceFuncNode(SetTraceFuncNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass setTraceFunc(RubyNilClass nil) {
            notDesignedForCompilation();

            getContext().getTraceManager().setTraceFunc(null);
            return nil;
        }

        @Specialization
        public RubyProc setTraceFunc(RubyProc traceFunc) {
            notDesignedForCompilation();

            getContext().getTraceManager().setTraceFunc(traceFunc);
            return traceFunc;
        }
    }

    @CoreMethod(names = "singleton_class")
    public abstract static class SingletonClassMethodNode extends CoreMethodNode {

        @Child private SingletonClassNode singletonClassNode;

        public SingletonClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            singletonClassNode = SingletonClassNodeFactory.create(context, sourceSection, null);
        }

        public SingletonClassMethodNode(SingletonClassMethodNode prev) {
            super(prev);
            singletonClassNode = prev.singletonClassNode;
        }

        @Specialization
        public RubyClass singletonClass(VirtualFrame frame, Object self) {
            return singletonClassNode.executeSingletonClass(frame, self);
        }

    }

    @CoreMethod(names = "singleton_methods", optional = 1)
    public abstract static class SingletonMethodsNode extends CoreMethodNode {

        public SingletonMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SingletonMethodsNode(SingletonMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray singletonMethods(RubyBasicObject self, boolean includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getContext().getCoreLibrary().getArrayClass());

            final Collection<InternalMethod> methods;

            if (includeInherited) {
                methods = ModuleOperations.getAllMethods(self.getSingletonClass(this)).values();
            } else {
                methods = self.getSingletonClass(this).getMethods().values();
            }

            for (InternalMethod method : methods) {
                array.slowPush(RubySymbol.newSymbol(self.getContext(), method.getName()));
            }

            return array;
        }

        @Specialization
        public RubyArray singletonMethods(RubyBasicObject self, UndefinedPlaceholder includeInherited) {
            return singletonMethods(self, false);
        }

    }

    @CoreMethod(names = "String", isModuleFunction = true, required = 1)
    public abstract static class StringNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode toS;

        public StringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public StringNode(StringNode prev) {
            super(prev);
            toS = prev.toS;
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
    public abstract static class SleepNode extends CoreMethodNode {

        @Child NumericToFloatNode floatCastNode;

        public SleepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SleepNode(SleepNode prev) {
            super(prev);
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
                floatCastNode = insert(NumericToFloatNodeFactory.create(getContext(), getSourceSection(), "to_f", null));
            }
            return sleep(floatCastNode.executeFloat(frame, duration));
        }

        @TruffleBoundary
        private long doSleepMillis(final long durationInMillis) {
            if (durationInMillis < 0) {
                throw new RaiseException(getContext().getCoreLibrary().argumentError("time interval must be positive", this));
            }

            final long start = System.currentTimeMillis();

            getContext().getThreadManager().runOnce(new BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    Thread.sleep(durationInMillis);
                    return SUCCESS;
                }
            });

            final long end = System.currentTimeMillis();

            return (end - start) / 1000;
        }

    }

    @CoreMethod(names = { "sprintf", "format" }, isModuleFunction = true, argumentsAsArray = true)
    public abstract static class SPrintfNode extends CoreMethodNode {

        public SPrintfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SPrintfNode(SPrintfNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyString sprintf(Object[] args) {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            final PrintStream printStream;

            try {
                printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            if (args.length > 0) {
                final String format = args[0].toString();
                final List<Object> values = Arrays.asList(args).subList(1, args.length);

                final RubyThread runningThread = getContext().getThreadManager().leaveGlobalLock();

                try {
                    // TODO(CS): this is only safe if values' toString() are pure.
                    StringFormatter.format(getContext(), printStream, format, values);
                } finally {
                    getContext().getThreadManager().enterGlobalLock(runningThread);
                }
            }

            return getContext().makeString(new ByteList(outputStream.toByteArray()));
        }
    }

    @CoreMethod(names = "system", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class SystemNode extends CoreMethodNode {

        public SystemNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SystemNode(SystemNode prev) {
            super(prev);
        }

        @Specialization
        public boolean system(RubyString command) {
            notDesignedForCompilation();

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
    public abstract static class KernelTaintNode extends CoreMethodNode {

        @Child private TaintNode taintNode;

        public KernelTaintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KernelTaintNode(KernelTaintNode prev) {
            super(prev);
        }

        @Specialization
        public Object taint(Object object) {
            if (taintNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintNode = insert(TaintNodeFactory.create(getContext(), getEncapsulatingSourceSection(), null));
            }
            return taintNode.executeTaint(object);
        }

    }

    @CoreMethod(names = "tainted?")
    public abstract static class KernelIsTaintedNode extends CoreMethodNode {

        @Child private IsTaintedNode isTaintedNode;

        public KernelIsTaintedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KernelIsTaintedNode(KernelIsTaintedNode prev) {
            super(prev);
            isTaintedNode = prev.isTaintedNode;
        }

        @Specialization
        public boolean isTainted(Object object) {
            if (isTaintedNode == null) {
                CompilerDirectives.transferToInterpreter();
                isTaintedNode = insert(IsTaintedNodeFactory.create(getContext(), getEncapsulatingSourceSection(), null));
            }
            return isTaintedNode.executeIsTainted(object);
        }

    }

    public abstract static class ToHexStringNode extends CoreMethodNode {

        public ToHexStringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToHexStringNode(ToHexStringNode prev) {
            super(prev);
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

        @Specialization
        public String toHexString(RubyBignum value) {
            return value.bigIntegerValue().toString(16);
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends CoreMethodNode {

        @Child private ClassNode classNode;
        @Child private ObjectPrimitiveNodes.ObjectIDPrimitiveNode objectIDNode;
        @Child private ToHexStringNode toHexStringNode;

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeFactory.create(context, sourceSection, null);
            objectIDNode = ObjectPrimitiveNodesFactory.ObjectIDPrimitiveNodeFactory.create(context, sourceSection, new RubyNode[] { null });
            toHexStringNode = KernelNodesFactory.ToHexStringNodeFactory.create(context, sourceSection, new RubyNode[]{null});
        }

        public abstract RubyString executeToS(VirtualFrame frame, Object self);

        @Specialization
        public RubyString toS(VirtualFrame frame, Object self) {
            notDesignedForCompilation();

            String className = classNode.executeGetClass(frame, self).getName();
            Object id = objectIDNode.executeObjectID(frame, self);
            String hexID = toHexStringNode.executeToHexString(frame, id);

            return getContext().makeString("#<" + className + ":0x" + hexID + ">");
        }

    }

    @CoreMethod(names = "untaint")
    public abstract static class UntaintNode extends CoreMethodNode {

        @Child private WriteHeadObjectFieldNode writeTaintNode;

        public UntaintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeTaintNode = new WriteHeadObjectFieldNode(RubyBasicObject.TAINTED_IDENTIFIER);
        }

        public UntaintNode(UntaintNode prev) {
            super(prev);
            writeTaintNode = prev.writeTaintNode;
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
