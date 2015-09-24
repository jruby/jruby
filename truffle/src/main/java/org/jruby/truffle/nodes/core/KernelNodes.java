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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyString;
import org.jruby.exceptions.MainExitException;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.StringCachingGuards;
import org.jruby.truffle.nodes.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.nodes.cast.NumericToFloatNode;
import org.jruby.truffle.nodes.cast.NumericToFloatNodeGen;
import org.jruby.truffle.nodes.coerce.NameToJavaStringNodeGen;
import org.jruby.truffle.nodes.coerce.ToPathNodeGen;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.core.KernelNodesFactory.CopyNodeFactory;
import org.jruby.truffle.nodes.core.KernelNodesFactory.SameOrEqualNodeFactory;
import org.jruby.truffle.nodes.core.KernelNodesFactory.SingletonMethodsNodeFactory;
import org.jruby.truffle.nodes.core.ProcNodes.ProcNewNode;
import org.jruby.truffle.nodes.core.ProcNodesFactory.ProcNewNodeFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DoesRespondDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.methods.LookupMethodNode;
import org.jruby.truffle.nodes.methods.LookupMethodNodeGen;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNodeGen;
import org.jruby.truffle.nodes.rubinius.ObjectPrimitiveNodes;
import org.jruby.truffle.nodes.rubinius.ObjectPrimitiveNodesFactory;
import org.jruby.truffle.pack.parser.FormatParser;
import org.jruby.truffle.pack.runtime.PackResult;
import org.jruby.truffle.pack.runtime.exceptions.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.ArrayOperations;
import org.jruby.truffle.runtime.core.MethodFilter;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.layouts.ThreadBacktraceLocationLayoutImpl;
import org.jruby.truffle.runtime.loader.FeatureLoader;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingAction;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@CoreClass(name = "Kernel")
public abstract class KernelNodes {

    @CoreMethod(names = "`", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class BacktickNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toHashNode;

        public BacktickNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(command)")
        public DynamicObject backtick(VirtualFrame frame, DynamicObject command) {
            // Command is lexically a string interoplation, so variables will already have been expanded

            if (toHashNode == null) {
                CompilerDirectives.transferToInterpreter();
                toHashNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            CompilerDirectives.transferToInterpreter();

            final RubyContext context = getContext();

            final DynamicObject env = context.getCoreLibrary().getENV();
            final DynamicObject envAsHash = (DynamicObject) toHashNode.call(frame, env, "to_hash", null);

            final List<String> envp = new ArrayList<>();

            // TODO(CS): cast
            for (Map.Entry<Object, Object> keyValue : HashOperations.iterableKeyValues(envAsHash)) {
                envp.add(keyValue.getKey().toString() + "=" + keyValue.getValue().toString());
            }

            final Process process;

            try {
                // We need to run via bash to get the variable and other expansion we expect
                process = Runtime.getRuntime().exec(new String[]{ "bash", "-c", command.toString() }, envp.toArray(new String[envp.size()]));
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
            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist(resultBuilder.toString(), Layouts.ENCODING.getEncoding(EncodingNodes.getEncoding("UTF-8"))), StringSupport.CR_UNKNOWN, null);
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
                equalNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
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
        public DynamicObject equal(Object other) {
            return nil();
        }

    }

    @CoreMethod(names = "!~", required = 1)
    public abstract static class NotMatchNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode matchNode;

        public NotMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            matchNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public boolean notMatch(VirtualFrame frame, Object self, Object other) {
            return !matchNode.callBoolean(frame, self, "=~", null, other);
        }

    }

    @CoreMethod(names = { "<=>" }, required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        @Child private SameOrEqualNode equalNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{ null, null });
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

    @CoreMethod(names = "abort", isModuleFunction = true, optional = 1)
    public abstract static class AbortNode extends CoreMethodArrayArgumentsNode {

        public AbortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(message)")
        public DynamicObject abort(DynamicObject message) {
            System.err.println(message.toString());
            return abort();
        }

        @Specialization
        public DynamicObject abort(NotProvided message) {
            return abort();
        }

        @TruffleBoundary
        private DynamicObject abort() {
            getContext().innerShutdown(false);
            throw new MainExitException(1, true);
        }
    }

    @CoreMethod(names = "binding", isModuleFunction = true)
    public abstract static class BindingNode extends CoreMethodArrayArgumentsNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject binding() {
            // Materialize the caller's frame - false means don't use a slow path to get it - we want to optimize it
            final MaterializedFrame callerFrame = RubyCallStack.getCallerFrame(getContext())
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();

            return BindingNodes.createBinding(getContext(), callerFrame);
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
        public DynamicObject calleeName() {
            CompilerDirectives.transferToInterpreter();
            // the "called name" of a method.
            return getSymbol(RubyCallStack.getCallingMethod(getContext()).getName());
        }
    }

    @CoreMethod(names = "caller_locations", isModuleFunction = true, optional = 2, lowerFixnumParameters = { 0, 1 })
    public abstract static class CallerLocationsNode extends CoreMethodArrayArgumentsNode {

        public CallerLocationsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject callerLocations(NotProvided omit, NotProvided length) {
            return callerLocations(1, -1);
        }

        @Specialization
        public DynamicObject callerLocations(int omit, NotProvided length) {
            return callerLocations(omit, -1);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject callerLocations(int omit, int length) {
            final DynamicObject threadBacktraceLocationClass = getContext().getCoreLibrary().getThreadBacktraceLocationClass();

            final Backtrace backtrace = RubyCallStack.getBacktrace(this, 1 + omit, true);

            int locationsCount = backtrace.getActivations().size();

            if (length != -1 && locationsCount > length) {
                locationsCount = length;
            }

            final Object[] locations = new Object[locationsCount];

            for (int n = 0; n < locationsCount; n++) {
                Activation activation = backtrace.getActivations().get(n);
                locations[n] = ThreadBacktraceLocationLayoutImpl.INSTANCE.createThreadBacktraceLocation(Layouts.CLASS.getInstanceFactory(threadBacktraceLocationClass), activation);
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), locations, locations.length);
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
        public DynamicObject getClass(VirtualFrame frame, Object self) {
            return classNode.executeGetClass(frame, self);
        }

    }

    public abstract static class CopyNode extends UnaryCoreMethodNode {

        @Child private CallDispatchHeadNode allocateNode;

        public CopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = DispatchHeadNodeFactory.createMethodCall(context, true);
        }

        public abstract DynamicObject executeCopy(VirtualFrame frame, DynamicObject self);

        @Specialization
        public DynamicObject copy(VirtualFrame frame, DynamicObject self) {
            final DynamicObject rubyClass = Layouts.BASIC_OBJECT.getLogicalClass(self);
            final DynamicObject newObject = (DynamicObject) allocateNode.call(frame, rubyClass, "allocate", null);
            copyInstanceVariables(self, newObject);
            return newObject;
        }

        @TruffleBoundary
        private void copyInstanceVariables(DynamicObject from, DynamicObject to) {
            for (Property property : from.getShape().getProperties()) {
                if (property.getKey() instanceof String) {
                    to.define(property.getKey(), property.get(from, from.getShape()), 0);
                }
            }
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
            initializeCloneNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
            isFrozenNode = IsFrozenNodeGen.create(context, sourceSection, null);
            freezeNode = FreezeNodeGen.create(context, sourceSection, null);
            singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject clone(VirtualFrame frame, DynamicObject self) {
            CompilerDirectives.transferToInterpreter();

            final DynamicObject newObject = copyNode.executeCopy(frame, self);

            // Copy the singleton class if any.
            if (Layouts.CLASS.getIsSingleton(Layouts.BASIC_OBJECT.getMetaClass(self))) {
                Layouts.MODULE.getFields(singletonClassNode.executeSingletonClass(frame, newObject)).initCopy(Layouts.BASIC_OBJECT.getMetaClass(self));
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
            initializeDupNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        @Specialization
        public DynamicObject dup(VirtualFrame frame, DynamicObject self) {
            final DynamicObject newObject = copyNode.executeCopy(frame, self);

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
    @ImportStatic(StringCachingGuards.class)
    public abstract static class EvalNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode toStr;
        @Child private BindingNode bindingNode;

        public EvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("source")
        public RubyNode coerceSourceToString(RubyNode source) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), source);
        }

        protected DynamicObject getCallerBinding(VirtualFrame frame) {
            if (bindingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                bindingNode = insert(KernelNodesFactory.BindingNodeFactory.create(
                        getContext(), getSourceSection(), new RubyNode[]{}));
            }

            try {
                return bindingNode.executeDynamicObject(frame);
            } catch (UnexpectedResultException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        protected static class RootNodeWrapper {
            private final RubyRootNode rootNode;

            public RootNodeWrapper(RubyRootNode rootNode) {
                this.rootNode = rootNode;
            }

            public RubyRootNode getRootNode() {
                return rootNode;
            }
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "byteListsEqual(source, cachedSource)",
                "!parseDependsOnDeclarationFrame(cachedRootNode)"
        }, limit = "getCacheLimit()")
        public Object evalNoBindingCached(
                VirtualFrame frame,
                DynamicObject source,
                NotProvided binding,
                NotProvided filename,
                NotProvided lineNumber,
                @Cached("privatizeByteList(source)") ByteList cachedSource,
                @Cached("compileSource(frame, source)") RootNodeWrapper cachedRootNode,
                @Cached("createCallTarget(cachedRootNode)") CallTarget cachedCallTarget,
                @Cached("create(cachedCallTarget)") DirectCallNode callNode
        ) {
            final DynamicObject callerBinding = getCallerBinding(frame);

            final MaterializedFrame parentFrame = Layouts.BINDING.getFrame(callerBinding);
            final Object callerSelf = RubyArguments.getSelf(parentFrame.getArguments());

            final InternalMethod method = new InternalMethod(
                    cachedRootNode.getRootNode().getSharedMethodInfo(),
                    cachedRootNode.getRootNode().getSharedMethodInfo().getName(),
                    getContext().getCoreLibrary().getObjectClass(),
                    Visibility.PUBLIC,
                    false,
                    cachedCallTarget,
                    parentFrame);

            return callNode.call(frame, RubyArguments.pack(
                    method,
                    parentFrame,
                    callerSelf,
                    null,
                    new Object[]{}));
        }

        @Specialization(guards = {
                "isRubyString(source)"
        }, contains = "evalNoBindingCached")
        public Object evalNoBindingUncached(VirtualFrame frame, DynamicObject source, NotProvided binding,
                                            NotProvided filename, NotProvided lineNumber) {
            return getContext().eval(Layouts.STRING.getByteList(source), getCallerBinding(frame), true, this);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isNil(noBinding)",
                "isRubyString(filename)"
        })
        public Object evalNilBinding(VirtualFrame frame, DynamicObject source, DynamicObject noBinding,
                                     DynamicObject filename, int lineNumber) {
            return evalNoBindingUncached(frame, source, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)"
        })
        public Object evalBinding(DynamicObject source, DynamicObject binding, NotProvided filename,
                                  NotProvided lineNumber) {
            final Object result = getContext().eval(Layouts.STRING.getByteList(source), binding, false, this);
            assert result != null;
            return result;
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isNil(noFilename)",
                "isNil(noLineNumber)"
        })
        public Object evalBinding(DynamicObject source, DynamicObject binding, DynamicObject noFilename, DynamicObject noLineNumber) {
            return evalBinding(source, binding, NotProvided.INSTANCE, NotProvided.INSTANCE);
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isRubyString(filename)" })
        public Object evalBindingFilename(DynamicObject source, DynamicObject binding, DynamicObject filename,
                                          NotProvided lineNumber) {
            return getContext().eval(Layouts.STRING.getByteList(source), binding, false, filename.toString(), this);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isRubyString(filename)",
                "isNil(noLineNumber)"
        })
        public Object evalBindingFilename(DynamicObject source, DynamicObject binding, DynamicObject filename, DynamicObject noLineNumber) {
            return evalBindingFilename(source, binding, filename, NotProvided.INSTANCE);
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isRubyString(filename)" })
        public Object evalBindingFilenameLine(DynamicObject source, DynamicObject binding, DynamicObject filename,
                                              int lineNumber) {
            return getContext().eval(Layouts.STRING.getByteList(source), binding, false, filename.toString(), this);
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyString(source)",
                "!isRubyBinding(badBinding)" })
        public Object evalBadBinding(DynamicObject source, DynamicObject badBinding, NotProvided filename,
                                     NotProvided lineNumber) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorWrongArgumentType(badBinding, "binding", this));
        }

        protected RootNodeWrapper compileSource(VirtualFrame frame, DynamicObject sourceText) {
            assert RubyGuards.isRubyString(sourceText);

            final DynamicObject callerBinding = getCallerBinding(frame);
            final MaterializedFrame parentFrame = Layouts.BINDING.getFrame(callerBinding);

            final Source source = Source.fromText(sourceText.toString(), "(eval)");

            final TranslatorDriver translator = new TranslatorDriver(getContext());

            return new RootNodeWrapper(translator.parse(getContext(), source, UTF8Encoding.INSTANCE, TranslatorDriver.ParserContext.EVAL, parentFrame, true, this, new NodeWrapper() {
                @Override
                public RubyNode wrap(RubyNode node) {
                    return node; // return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), Visibility.PRIVATE, "simple eval", node);
                }
            }));
        }

        protected boolean parseDependsOnDeclarationFrame(RootNodeWrapper rootNode) {
            return rootNode.getRootNode().needsDeclarationFrame();
        }

        protected CallTarget createCallTarget(RootNodeWrapper rootNode) {
            return Truffle.getRuntime().createCallTarget(rootNode.rootNode);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().EVAL_CACHE;
        }

    }

    @CoreMethod(names = "exec", isModuleFunction = true, required = 1, rest = true)
    public abstract static class ExecNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toHashNode;

        public ExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object exec(VirtualFrame frame, Object command, Object[] args) {
            if (toHashNode == null) {
                CompilerDirectives.transferToInterpreter();
                toHashNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            CompilerDirectives.transferToInterpreter();

            final String[] commandLine = new String[1 + args.length];
            commandLine[0] = command.toString();
            for (int n = 0; n < args.length; n++) {
                commandLine[1 + n] = args[n].toString();
            }

            final DynamicObject env = getContext().getCoreLibrary().getENV();
            final DynamicObject envAsHash = (DynamicObject) toHashNode.call(frame, env, "to_hash", null);

            exec(getContext(), envAsHash, commandLine);

            return null;
        }

        @TruffleBoundary
        private void exec(RubyContext context, DynamicObject envAsHash, String[] commandLine) {
            final ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.inheritIO();

            for (Map.Entry<Object, Object> keyValue : HashOperations.iterableKeyValues(envAsHash)) {
                builder.environment().put(keyValue.getKey().toString(), keyValue.getValue().toString());
            }

            final Process process;

            try {
                process = builder.start();
            } catch (IOException e) {
                // TODO(cs): proper Ruby exception
                throw new RuntimeException(e);
            }

            int exitCode = context.getThreadManager().runUntilResult(this, new BlockingAction<Integer>() {
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
        public Object exit(NotProvided exitCode) {
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
        public DynamicObject exit(NotProvided exitCode) {
            return exit(1);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject exit(int exitCode) {
            getContext().innerShutdown(false);
            throw new MainExitException(exitCode, true);
        }

    }

    @CoreMethod(names = "fork", isModuleFunction = true, rest = true)
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
        public DynamicObject gets(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS): having some trouble interacting with JRuby stdin - so using this hack
            final InputStream in = getContext().getRuntime().getInstanceConfig().getInput();

            Encoding encoding = getContext().getRuntime().getDefaultExternalEncoding();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding.getCharset()));

            final String line = getContext().getThreadManager().runUntilResult(this, new BlockingAction<String>() {
                @Override
                public String block() throws InterruptedException {
                    return gets(reader);
                }
            });

            final DynamicObject rubyLine = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist(line, UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null);

            // Set the local variable $_ in the caller

            final Frame caller = RubyCallStack.getCallerFrame(getContext()).getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

            final FrameSlot slot = caller.getFrameDescriptor().findFrameSlot("$_");

            if (slot != null) {
                caller.setObject(slot, ThreadLocalObject.wrap(getContext(), rubyLine));
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

        @TruffleBoundary
        @Specialization
        public int hash(DynamicObject self) {
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
        public Object initializeCopy(DynamicObject self, DynamicObject from) {
            CompilerDirectives.transferToInterpreter();

            if (Layouts.BASIC_OBJECT.getLogicalClass(self) != Layouts.BASIC_OBJECT.getLogicalClass(from)) {
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
        public Object initializeDup(VirtualFrame frame, DynamicObject self, DynamicObject from) {
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

        @Specialization(guards = "isRubyClass(rubyClass)")
        public boolean instanceOf(VirtualFrame frame, Object self, DynamicObject rubyClass) {
            return classNode.executeGetClass(frame, self) == rubyClass;
        }

    }

    @CoreMethod(names = "instance_variable_defined?", required = 1)
    public abstract static class InstanceVariableDefinedNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(name)")
        public boolean isInstanceVariableDefinedString(DynamicObject object, DynamicObject name) {
            return object.getShape().hasProperty(RubyContext.checkInstanceVariableName(getContext(), name.toString(), this));
        }

        @TruffleBoundary
        @Specialization(guards = "isRubySymbol(name)")
        public boolean isInstanceVariableDefinedSymbol(DynamicObject object, DynamicObject name) {
            return object.getShape().hasProperty(RubyContext.checkInstanceVariableName(getContext(), Layouts.SYMBOL.getString(name), this));
        }

    }

    @CoreMethod(names = "instance_variable_get", required = 1)
    public abstract static class InstanceVariableGetNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(name)")
        public Object instanceVariableGetString(DynamicObject object, DynamicObject name) {
            return instanceVariableGet(object, name.toString());
        }

        @TruffleBoundary
        @Specialization(guards = "isRubySymbol(name)")
        public Object instanceVariableGetSymbol(DynamicObject object, DynamicObject name) {
            return instanceVariableGet(object, Layouts.SYMBOL.getString(name));
        }

        private Object instanceVariableGet(DynamicObject object, String name) {
            return object.get(RubyContext.checkInstanceVariableName(getContext(), name, this), nil());
        }

    }

    @CoreMethod(names = { "instance_variable_set", "__instance_variable_set__" }, raiseIfFrozenSelf = true, required = 2)
    public abstract static class InstanceVariableSetNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        // TODO CS 4-Mar-15 this badly needs to be cached

        @TruffleBoundary
        @Specialization(guards = "isRubyString(name)")
        public Object instanceVariableSetString(DynamicObject object, DynamicObject name, Object value) {
            object.define(RubyContext.checkInstanceVariableName(getContext(), name.toString(), this), value, 0);
            return value;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubySymbol(name)")
        public Object instanceVariableSetSymbol(DynamicObject object, DynamicObject name, Object value) {
            object.define(RubyContext.checkInstanceVariableName(getContext(), Layouts.SYMBOL.getString(name), this), value, 0);
            return value;
        }

    }

    @CoreMethod(names = { "instance_variables", "__instance_variables__" })
    public abstract static class InstanceVariablesNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject instanceVariables(DynamicObject self) {
            List<Object> keys = self.getShape().getKeyList();
            final Object[] instanceVariableNames = keys.toArray(new Object[keys.size()]);

            Arrays.sort(instanceVariableNames);

            final DynamicObject array = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);

            for (Object name : instanceVariableNames) {
                if (name instanceof String) {
                    ArrayOperations.append(array, getSymbol((String) name));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = { "is_a?", "kind_of?" }, required = 1)
    public abstract static class IsANode extends CoreMethodArrayArgumentsNode {

        @Child MetaClassNode metaClassNode;

        public IsANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        public abstract boolean executeIsA(VirtualFrame frame, Object self, DynamicObject rubyClass);

        @Specialization(guards = { "isNil(nil)", "!isRubyModule(nil)" })
        public boolean isANil(DynamicObject self, Object nil) {
            return false;
        }

        @Specialization(
                limit = "getCacheLimit()",
                guards = { "isRubyModule(module)", "getMetaClass(frame, self) == cachedMetaClass", "module == cachedModule" },
                assumptions = "getUnmodifiedAssumption(cachedModule)")
        public boolean isACached(VirtualFrame frame,
                                 Object self,
                                 DynamicObject module,
                                 @Cached("getMetaClass(frame, self)") DynamicObject cachedMetaClass,
                                 @Cached("module") DynamicObject cachedModule,
                                 @Cached("isA(cachedMetaClass, cachedModule)") boolean result) {
            return result;
        }

        public Assumption getUnmodifiedAssumption(DynamicObject module) {
            return Layouts.MODULE.getFields(module).getUnmodifiedAssumption();
        }

        @Specialization(guards = "isRubyModule(module)")
        public boolean isAUncached(VirtualFrame frame, Object self, DynamicObject module) {
            return isA(getMetaClass(frame, self), module);
        }

        @Specialization(guards = "!isRubyModule(module)")
        public boolean isATypeError(VirtualFrame frame, Object self, Object module) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeError("class or module required", this));
        }

        @TruffleBoundary
        protected boolean isA(DynamicObject metaClass, DynamicObject module) {
            return ModuleOperations.assignableTo(metaClass, module);
        }

        protected DynamicObject getMetaClass(VirtualFrame frame, Object object) {
            return metaClassNode.executeMetaClass(frame, object);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().IS_A_CACHE;
        }

    }

    @CoreMethod(names = "lambda", isModuleFunction = true, needsBlock = true)
    public abstract static class LambdaNode extends CoreMethodArrayArgumentsNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject lambda(NotProvided block) {
            final Frame parentFrame = RubyCallStack.getCallerFrame(getContext()).getFrame(FrameAccess.READ_ONLY, true);
            final DynamicObject parentBlock = RubyArguments.getBlock(parentFrame.getArguments());

            if (parentBlock == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("tried to create Proc object without a block", this));
            }
            return lambda(parentBlock);
        }

        @Specialization(guards = "isRubyProc(block)")
        public DynamicObject lambda(DynamicObject block) {
            return ProcNodes.createRubyProc(
                    getContext().getCoreLibrary().getProcFactory(),
                    ProcNodes.Type.LAMBDA,
                    Layouts.PROC.getSharedMethodInfo(block),
                    Layouts.PROC.getCallTargetForLambdas(block),
                    Layouts.PROC.getCallTargetForLambdas(block),
                    Layouts.PROC.getDeclarationFrame(block),
                    Layouts.PROC.getMethod(block),
                    Layouts.PROC.getSelf(block),
                    Layouts.PROC.getBlock(block));
        }
    }

    @CoreMethod(names = "load", isModuleFunction = true, required = 1, optional = 1)
    public abstract static class LoadNode extends CoreMethodArrayArgumentsNode {

        public LoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(file)")
        public boolean load(DynamicObject file, boolean wrap) {
            if (wrap) {
                throw new UnsupportedOperationException();
            }

            try {
                getContext().loadFile(file.toString(), this);
            } catch (IOException e) {
                throw new RaiseException(getContext().getCoreLibrary().loadErrorCannotLoad(file.toString(), this));
            }

            return true;
        }

        @Specialization(guards = "isRubyString(file)")
        public boolean load(DynamicObject file, NotProvided wrap) {
            return load(file, false);
        }
    }

    @CoreMethod(names = "local_variables", needsSelf = false)
    public abstract static class LocalVariablesNode extends CoreMethodArrayArgumentsNode {

        public LocalVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject localVariables() {
            final DynamicObject array = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);

            Frame frame = RubyCallStack.getCallerFrame(getContext()).getFrame(FrameInstance.FrameAccess.READ_ONLY, false);

            while (frame != null) {
                for (Object name : frame.getFrameDescriptor().getIdentifiers()) {
                    if (name instanceof String) {
                        ArrayOperations.append(array, getSymbol((String) name));
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
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
        public DynamicObject methodName() {
            CompilerDirectives.transferToInterpreter();
            // the "original/definition name" of the method.
            return getSymbol(RubyCallStack.getCallingMethod(getContext()).getSharedMethodInfo().getName());
        }

    }

    @CoreMethod(names = "method", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class MethodNode extends CoreMethodNode {

        @Child LookupMethodNode lookupMethodNode;

        public MethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            lookupMethodNode = LookupMethodNodeGen.create(context, sourceSection, null, null);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public DynamicObject methodCached(VirtualFrame frame, Object self, String name) {
            InternalMethod method = lookupMethodNode.executeLookupMethod(frame, self, name);

            if (method == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUndefinedMethod(
                        name, getContext().getCoreLibrary().getLogicalClass(self), this));
            }

            return Layouts.METHOD.createMethod(getContext().getCoreLibrary().getMethodFactory(), self, method);
        }

    }

    @CoreMethod(names = "methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "regular")
    })
    public abstract static class MethodsNode extends CoreMethodNode {

        public MethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("regular")
        public RubyNode coerceToBoolean(RubyNode regular) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, regular);
        }

        @Specialization(guards = "regular")
        public DynamicObject methodsRegular(VirtualFrame frame, Object self, boolean regular,
                                            @Cached("createMetaClassNode()") MetaClassNode metaClassNode) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(regular, MethodFilter.PUBLIC_PROTECTED).toArray();
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length);
        }

        @Specialization(guards = "!regular")
        public DynamicObject methodsSingleton(VirtualFrame frame, Object self, boolean regular,
                                              @Cached("createSingletonMethodsNode()") SingletonMethodsNode singletonMethodsNode) {
            return singletonMethodsNode.executeSingletonMethods(frame, self, false);
        }

        protected MetaClassNode createMetaClassNode() {
            return MetaClassNodeGen.create(getContext(), getSourceSection(), null);
        }

        protected SingletonMethodsNode createSingletonMethodsNode() {
            return SingletonMethodsNodeFactory.create(getContext(), getSourceSection(), null, null);
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
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class PrivateMethodsNode extends CoreMethodNode {

        @Child private MetaClassNode metaClassNode;

        public PrivateMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public DynamicObject privateMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            DynamicObject metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(includeAncestors, MethodFilter.PRIVATE).toArray();
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length);
        }

    }

    @CoreMethod(names = "proc", isModuleFunction = true, needsBlock = true)
    public abstract static class ProcNode extends CoreMethodArrayArgumentsNode {

        @Child ProcNewNode procNewNode;

        public ProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            procNewNode = ProcNewNodeFactory.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject proc(VirtualFrame frame, Object maybeBlock) {
            return procNewNode.executeProcNew(frame, getContext().getCoreLibrary().getProcClass(), ArrayUtils.EMPTY_ARRAY, maybeBlock);
        }

    }

    @CoreMethod(names = "protected_methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class ProtectedMethodsNode extends CoreMethodNode {

        @Child private MetaClassNode metaClassNode;

        public ProtectedMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public DynamicObject protectedMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(includeAncestors, MethodFilter.PROTECTED).toArray();
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length);
        }

    }

    @CoreMethod(names = "public_methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class PublicMethodsNode extends CoreMethodNode {

        @Child private MetaClassNode metaClassNode;

        public PublicMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public DynamicObject publicMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(frame, self);

            CompilerDirectives.transferToInterpreter();
            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(includeAncestors, MethodFilter.PUBLIC).toArray();
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length);
        }

    }

    @CoreMethod(names = "public_send", needsBlock = true, required = 1, rest = true)
    public abstract static class PublicSendNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode dispatchNode;

        public PublicSendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatchNode = new CallDispatchHeadNode(context, false,
                    MissingBehavior.CALL_METHOD_MISSING);
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, NotProvided block) {
            return send(frame, self, name, args, (DynamicObject) null);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, DynamicObject block) {
            return dispatchNode.call(frame, self, name, block, args);
        }

    }

    @CoreMethod(names = "rand", isModuleFunction = true, optional = 1)
    public abstract static class RandNode extends CoreMethodArrayArgumentsNode {

        public RandNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double rand(NotProvided max) {
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
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "feature")
    })
    public abstract static class RequireNode extends CoreMethodNode {

        public RequireNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("feature")
        public RubyNode coerceFeatureToPath(RubyNode feature) {
            return ToPathNodeGen.create(getContext(), getSourceSection(), feature);
        }

        @Specialization(guards = "isRubyString(featureString)")
        public boolean require(DynamicObject featureString) {
            final String feature = featureString.toString();

            // Pysch loads either the jar or the so - we need to intercept

            if (feature.equals("psych.so") && RubyCallStack.getCallerFrame(getContext()).getCallNode()
                    .getEncapsulatingSourceSection().getSource().getName().endsWith("psych.rb")) {
                try {
                    getContext().getFeatureLoader().require("truffle/psych.rb", this);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return true;
            }

            // TODO CS 1-Mar-15 ERB will use strscan if it's there, but strscan is not yet complete, so we need to hide it

            if (feature.equals("strscan") && RubyCallStack.getCallerFrame(getContext()).getCallNode()
                    .getEncapsulatingSourceSection().getSource().getName().endsWith("erb.rb")) {
                throw new RaiseException(getContext().getCoreLibrary().loadErrorCannotLoad(feature, this));
            }

            // TODO CS 19-May-15 securerandom will use openssl if it's there, but we've only shimmed it

            if (feature.equals("openssl") && RubyCallStack.getCallerFrame(getContext()).getCallNode()
                    .getEncapsulatingSourceSection().getSource().getName().endsWith("securerandom.rb")) {
                Layouts.MODULE.getFields(getContext().getCoreLibrary().getObjectClass()).getConstants().remove("OpenSSL");
                throw new RaiseException(getContext().getCoreLibrary().loadErrorCannotLoad(feature, this));
            }

            try {
                getContext().getFeatureLoader().require(feature, this);
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

        @TruffleBoundary
        @Specialization(guards = "isRubyString(feature)")
        public boolean requireRelative(DynamicObject feature) {
            final FeatureLoader featureLoader = getContext().getFeatureLoader();

            final String featureString = feature.toString();
            final String featurePath;

            if (featureLoader.isAbsolutePath(featureString)) {
                featurePath = featureString;
            } else {
                final Source source = RubyCallStack.getCallerFrame(getContext()).getCallNode().getEncapsulatingSourceSection().getSource();
                final String sourcePath = featureLoader.getSourcePath(source);

                if (sourcePath == null) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().loadError("cannot infer basepath", this));
                }

                featurePath = dirname(sourcePath) + "/" + featureString;
            }

            try {
                featureLoader.require(featurePath, this);
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
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "includeProtectedAndPrivate")
    })
    public abstract static class RespondToNode extends CoreMethodNode {

        @Child private DoesRespondDispatchHeadNode dispatch;
        @Child private DoesRespondDispatchHeadNode dispatchIgnoreVisibility;
        @Child private CallDispatchHeadNode respondToMissingNode;
        private final ConditionProfile ignoreVisibilityProfile = ConditionProfile.createBinaryProfile();

        public RespondToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatch = new DoesRespondDispatchHeadNode(context, false, MissingBehavior.RETURN_MISSING, null);
            dispatchIgnoreVisibility = new DoesRespondDispatchHeadNode(context, true, MissingBehavior.RETURN_MISSING, null);
        }

        public abstract boolean executeDoesRespondTo(VirtualFrame frame, Object object, Object name, boolean includeProtectedAndPrivate);

        @CreateCast("includeProtectedAndPrivate")
        public RubyNode coerceToBoolean(RubyNode includeProtectedAndPrivate) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), false, includeProtectedAndPrivate);
        }

        @Specialization(guards = "isRubyString(name)")
        public boolean doesRespondToString(VirtualFrame frame, Object object, DynamicObject name, boolean includeProtectedAndPrivate) {
            final boolean ret;

            if (ignoreVisibilityProfile.profile(includeProtectedAndPrivate)) {
                ret = dispatchIgnoreVisibility.doesRespondTo(frame, name, object);
            } else {
                ret = dispatch.doesRespondTo(frame, name, object);
            }

            if (ret) {
                return true;
            } else {
                return respondToMissing(frame, object, name, includeProtectedAndPrivate);
            }
        }

        @Specialization(guards = "isRubySymbol(name)")
        public boolean doesRespondToSymbol(VirtualFrame frame, Object object, DynamicObject name, boolean includeProtectedAndPrivate) {
            final boolean ret;

            if (ignoreVisibilityProfile.profile(includeProtectedAndPrivate)) {
                ret = dispatchIgnoreVisibility.doesRespondTo(frame, name, object);
            } else {
                ret = dispatch.doesRespondTo(frame, name, object);
            }

            if (ret) {
                return true;
            } else {
                return respondToMissing(frame, object, name, includeProtectedAndPrivate);
            }
        }

        private boolean respondToMissing(VirtualFrame frame, Object object, DynamicObject name, boolean includeProtectedAndPrivate) {
            if (respondToMissingNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToMissingNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return respondToMissingNode.callBoolean(frame, object, "respond_to_missing?", null, name, includeProtectedAndPrivate);
        }
    }

    @CoreMethod(names = "respond_to_missing?", required = 2)
    public abstract static class RespondToMissingNode extends CoreMethodArrayArgumentsNode {

        public RespondToMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(name)")
        public boolean doesRespondToMissingString(Object object, DynamicObject name, Object unusedIncludeAll) {
            return false;
        }

        @Specialization(guards = "isRubySymbol(name)")
        public boolean doesRespondToMissingSymbol(Object object, DynamicObject name, Object unusedIncludeAll) {
            return false;
        }

    }

    @CoreMethod(names = "set_trace_func", isModuleFunction = true, required = 1)
    public abstract static class SetTraceFuncNode extends CoreMethodArrayArgumentsNode {

        public SetTraceFuncNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNil(nil)")
        public DynamicObject setTraceFunc(Object nil) {
            CompilerDirectives.transferToInterpreter();

            getContext().getTraceManager().setTraceFunc(null);
            return nil();
        }

        @Specialization(guards = "isRubyProc(traceFunc)")
        public DynamicObject setTraceFunc(DynamicObject traceFunc) {
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
        public DynamicObject singletonClass(VirtualFrame frame, Object self) {
            return singletonClassNode.executeSingletonClass(frame, self);
        }

    }

    @CoreMethod(names = "singleton_methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "includeAncestors")
    })
    public abstract static class SingletonMethodsNode extends CoreMethodNode {

        @Child private MetaClassNode metaClassNode;

        public SingletonMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
        }

        public abstract DynamicObject executeSingletonMethods(VirtualFrame frame, Object self, boolean includeAncestors);

        @CreateCast("includeAncestors")
        public RubyNode coerceToBoolean(RubyNode includeAncestors) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), true, includeAncestors);
        }

        @Specialization
        public DynamicObject singletonMethods(VirtualFrame frame, Object self, boolean includeAncestors) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(frame, self);

            if (!Layouts.CLASS.getIsSingleton(metaClass)) {
                return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
            }

            CompilerDirectives.transferToInterpreter();
            Object[] objects = Layouts.MODULE.getFields(metaClass).filterSingletonMethods(includeAncestors, MethodFilter.PUBLIC_PROTECTED).toArray();
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length);
        }

    }

    @CoreMethod(names = "String", isModuleFunction = true, required = 1)
    public abstract static class StringNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toS;

        public StringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization(guards = "isRubyString(value)")
        public DynamicObject string(DynamicObject value) {
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
        public long sleep(NotProvided duration) {
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
        public long sleep(DynamicObject duration) {
            return sleep(NotProvided.INSTANCE);
        }

        @Specialization(guards = "!isRubiniusUndefined(duration)")
        public long sleep(VirtualFrame frame, DynamicObject duration) {
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

            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();

            // Clear the wakeUp flag, following Ruby semantics:
            // it should only be considered if we are inside the sleep when Thread#{run,wakeup} is called.
            Layouts.THREAD.getWakeUp(thread).getAndSet(false);

            return sleepFor(this, getContext(), durationInMillis);
        }

        public static long sleepFor(Node currentNode, RubyContext context, final long durationInMillis) {
            assert durationInMillis >= 0;

            final DynamicObject thread = context.getThreadManager().getCurrentThread();

            final long start = System.currentTimeMillis();

            long slept = context.getThreadManager().runUntilResult(currentNode, new BlockingAction<Long>() {
                @Override
                public Long block() throws InterruptedException {
                    long now = System.currentTimeMillis();
                    long slept = now - start;

                    if (slept >= durationInMillis || Layouts.THREAD.getWakeUp(thread).getAndSet(false)) {
                        return slept;
                    }
                    Thread.sleep(durationInMillis - slept);

                    return System.currentTimeMillis() - start;
                }
            });

            return slept / 1000;
        }

    }

    @CoreMethod(names = { "format", "sprintf" }, isModuleFunction = true, rest = true, required = 1, taintFromParameter = 0)
    @ImportStatic(StringCachingGuards.class)
    public abstract static class FormatNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode;

        public FormatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isRubyString(format)", "byteListsEqual(format, cachedFormat)" })
        public DynamicObject formatCached(
                VirtualFrame frame,
                DynamicObject format,
                Object[] arguments,
                @Cached("privatizeByteList(format)") ByteList cachedFormat,
                @Cached("create(compileFormat(format))") DirectCallNode callPackNode) {
            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, new Object[]{ arguments, arguments.length });
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishFormat(cachedFormat, result);
        }

        @Specialization(guards = "isRubyString(format)", contains = "formatCached")
        public DynamicObject formatUncached(
                VirtualFrame frame,
                DynamicObject format,
                Object[] arguments,
                @Cached("create()") IndirectCallNode callPackNode) {
            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, compileFormat(format), new Object[]{ arguments, arguments.length });
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishFormat(Layouts.STRING.getByteList(format), result);
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

        private DynamicObject finishFormat(ByteList format, PackResult result) {
            final DynamicObject string = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(result.getOutput(), 0, result.getOutputLength()), StringSupport.CR_UNKNOWN, null);

            if (format.length() == 0) {
                StringOperations.forceEncoding(string, USASCIIEncoding.INSTANCE);
            } else {
                switch (result.getEncoding()) {
                    case DEFAULT:
                    case ASCII_8BIT:
                        break;
                    case US_ASCII:
                        StringOperations.forceEncoding(string, USASCIIEncoding.INSTANCE);
                        break;
                    case UTF_8:
                        StringOperations.forceEncoding(string, UTF8Encoding.INSTANCE);
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

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            assert RubyGuards.isRubyString(format);

            try {
                return new FormatParser(getContext()).parse(Layouts.STRING.getByteList(format));
            } catch (FormatException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(e.getMessage(), this));
            }
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
        public String toHexString(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).toString(16);
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @Child private ClassNode classNode;
        @Child private ObjectPrimitiveNodes.ObjectIDPrimitiveNode objectIDNode;
        @Child private ToHexStringNode toHexStringNode;

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeGen.create(context, sourceSection, null);
            objectIDNode = ObjectPrimitiveNodesFactory.ObjectIDPrimitiveNodeFactory.create(context, sourceSection, new RubyNode[]{ null });
            toHexStringNode = KernelNodesFactory.ToHexStringNodeFactory.create(context, sourceSection, new RubyNode[]{ null });
        }

        public abstract DynamicObject executeToS(VirtualFrame frame, Object self);

        @Specialization
        public DynamicObject toS(VirtualFrame frame, Object self) {
            CompilerDirectives.transferToInterpreter();

            String className = Layouts.MODULE.getFields(classNode.executeGetClass(frame, self)).getName();
            Object id = objectIDNode.executeObjectID(frame, self);
            String hexID = toHexStringNode.executeToHexString(frame, id);

            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("#<" + className + ":0x" + hexID + ">", UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null);
        }

    }

    @CoreMethod(names = "untaint")
    public abstract static class UntaintNode extends CoreMethodArrayArgumentsNode {

        @Child private IsFrozenNode isFrozenNode;
        @Child private IsTaintedNode isTaintedNode;
        @Child private WriteHeadObjectFieldNode writeTaintNode;

        public UntaintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isFrozenNode = IsFrozenNodeGen.create(context, sourceSection, null);
            isTaintedNode = IsTaintedNodeGen.create(context, sourceSection, null);
            writeTaintNode = WriteHeadObjectFieldNodeGen.create(Layouts.TAINTED_IDENTIFIER);
        }

        @Specialization
        public Object taint(DynamicObject object) {
            if (!isTaintedNode.executeIsTainted(object)) {
                return object;
            }

            if (isFrozenNode.executeIsFrozen(object)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().frozenError(Layouts.MODULE.getFields(getContext().getCoreLibrary().getLogicalClass(object)).getName(), this));
            }

            writeTaintNode.execute(object, false);
            return object;
        }

    }

}
