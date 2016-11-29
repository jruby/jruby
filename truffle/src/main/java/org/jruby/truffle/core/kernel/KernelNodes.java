/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.kernel;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.Hashing;
import org.jruby.truffle.core.ObjectNodes;
import org.jruby.truffle.core.ObjectNodesFactory;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.basicobject.BasicObjectNodes;
import org.jruby.truffle.core.basicobject.BasicObjectNodes.ReferenceEqualNode;
import org.jruby.truffle.core.basicobject.BasicObjectNodesFactory;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.core.cast.DurationToMillisecondsNodeGen;
import org.jruby.truffle.core.cast.NameToJavaStringNode;
import org.jruby.truffle.core.cast.NameToJavaStringNodeGen;
import org.jruby.truffle.core.cast.NameToSymbolOrStringNodeGen;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.cast.ToPathNodeGen;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.format.BytesResult;
import org.jruby.truffle.core.format.FormatExceptionTranslator;
import org.jruby.truffle.core.format.exceptions.FormatException;
import org.jruby.truffle.core.format.exceptions.InvalidFormatException;
import org.jruby.truffle.core.format.printf.PrintfCompiler;
import org.jruby.truffle.core.hash.HashOperations;
import org.jruby.truffle.core.hash.KeyValue;
import org.jruby.truffle.core.kernel.KernelNodesFactory.CopyNodeFactory;
import org.jruby.truffle.core.kernel.KernelNodesFactory.SameOrEqualNodeFactory;
import org.jruby.truffle.core.kernel.KernelNodesFactory.SingletonMethodsNodeFactory;
import org.jruby.truffle.core.method.MethodFilter;
import org.jruby.truffle.core.proc.ProcNodes.ProcNewNode;
import org.jruby.truffle.core.proc.ProcNodesFactory.ProcNewNodeFactory;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.symbol.SymbolTable;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.backtrace.Activation;
import org.jruby.truffle.language.backtrace.Backtrace;
import org.jruby.truffle.language.control.JavaException;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.DoesRespondDispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.loader.RequireNode;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.LookupMethodNode;
import org.jruby.truffle.language.methods.LookupMethodNodeGen;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.objects.FreezeNode;
import org.jruby.truffle.language.objects.FreezeNodeGen;
import org.jruby.truffle.language.objects.IsANode;
import org.jruby.truffle.language.objects.IsANodeGen;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;
import org.jruby.truffle.language.objects.IsTaintedNode;
import org.jruby.truffle.language.objects.IsTaintedNodeGen;
import org.jruby.truffle.language.objects.LogicalClassNode;
import org.jruby.truffle.language.objects.LogicalClassNodeGen;
import org.jruby.truffle.language.objects.MetaClassNode;
import org.jruby.truffle.language.objects.MetaClassNodeGen;
import org.jruby.truffle.language.objects.ObjectIVarGetNode;
import org.jruby.truffle.language.objects.ObjectIVarGetNodeGen;
import org.jruby.truffle.language.objects.ObjectIVarSetNode;
import org.jruby.truffle.language.objects.ObjectIVarSetNodeGen;
import org.jruby.truffle.language.objects.PropertyFlags;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.objects.SingletonClassNodeGen;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.objects.TaintNodeGen;
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;
import org.jruby.truffle.language.objects.shared.SharedObjects;
import org.jruby.truffle.language.threadlocal.ThreadLocalObject;
import org.jruby.truffle.parser.ParserContext;
import org.jruby.truffle.parser.TranslatorDriver;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jruby.truffle.core.array.ArrayHelpers.getStore;

@CoreClass("Kernel")
public abstract class KernelNodes {

    @CoreMethod(names = "`", isModuleFunction = true, required = 1, unsafe = { UnsafeGroup.IO, UnsafeGroup.PROCESSES })
    public abstract static class BacktickNode extends CoreMethodArrayArgumentsNode {

        private static class ExecuteResult {

            private final DynamicObject output;
            private final int pid;
            private final int code;

            public ExecuteResult(DynamicObject output, int pid, int code) {
                this.output = output;
                this.pid = pid;
                this.code = code;
            }

            public DynamicObject getOutput() {
                return output;
            }

            public int getPid() {
                return pid;
            }

            public int getCode() {
                return code;
            }
        }

        @Child private CallDispatchHeadNode toHashNode;
        @Child private ToStrNode toStrNode;
        @Child private SnippetNode setStatusNode = new SnippetNode();

        @Specialization(guards = "!isRubyString(command)")
        public DynamicObject backtickCoerce(VirtualFrame frame, DynamicObject command) {
            // TODO BJF Aug 4, 2016 Needs SafeStringValue here
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
            }
            return backtick(frame, toStrNode.executeToStr(frame, command));
        }

        @Specialization(guards = "isRubyString(command)")
        public DynamicObject backtick(VirtualFrame frame, DynamicObject command) {
            // Command is lexically a string interoplation, so variables will already have been expanded

            if (toHashNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toHashNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final DynamicObject env = getContext().getCoreLibrary().getENV();
            final DynamicObject envAsHash = (DynamicObject) toHashNode.call(frame, env, "to_hash");

            final ExecuteResult result = spawnAndCaptureOutput(command, envAsHash);

            setStatusNode.execute(frame,
                    "Rubinius::Mirror::Process.set_status_global Process::Status.new(pid, code)",
                    "pid", result.getPid(),
                    "code", result.getCode());

            return result.output;
        }

        @TruffleBoundary
        private ExecuteResult spawnAndCaptureOutput(DynamicObject command, final DynamicObject envAsHash) {
            if (TruffleOptions.AOT) {
                throw new UnsupportedOperationException("ProcessEnvironment.environment not supported with AOT");
            }

            // We need to run via bash to get the variable and other expansion we expect
            String[] cmdArray = new String[] { "bash", "-c", command.toString() };

            ProcessBuilder builder = new ProcessBuilder(cmdArray).redirectError(Redirect.INHERIT);

            Map<String, String> env = builder.environment();
            env.clear();
            for (KeyValue keyValue : HashOperations.iterableKeyValues(envAsHash)) {
                // TODO(CS): toString
                env.put(keyValue.getKey().toString(), keyValue.getValue().toString());
            }

            final Process process;
            try {
                process = builder.start();
            } catch (IOException e) {
                throw new JavaException(e);
            }

            final InputStream stdout = process.getInputStream();

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            final int bufferSize = 1024;
            final byte[] buffer = new byte[bufferSize];
            int bytesRead = 0;
            try {
                while ((bytesRead = stdout.read(buffer, 0, bufferSize)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new JavaException(e);
            }

            final int code = getContext().getThreadManager().runUntilResult(this, () -> process.waitFor());
            final DynamicObject output = createString(baos.toByteArray(), getContext().getEncodingManager().getDefaultExternalEncoding());

            // TODO CS 30-Oct-16 how to get the PID? JRuby does some gymnastics with reflection. I think we
            // should probably reimplement this in Ruby using spawn, which starts processes with JNR and so
            // has proper access to things like the PID.

            final int pid = 0;

            return new ExecuteResult(output, pid, code);
        }

    }

    /**
     * Check if operands are the same object or call #==.
     * Known as rb_equal() in MRI. The fact Kernel#=== uses this is pure coincidence.
     */
    @CoreMethod(names = "===", required = 1)
    public abstract static class SameOrEqualNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode equalNode;

        private final ConditionProfile sameProfile = ConditionProfile.createBinaryProfile();

        public abstract boolean executeSameOrEqual(VirtualFrame frame, Object a, Object b);

        @Specialization
        public boolean sameOrEqual(VirtualFrame frame, Object a, Object b,
                @Cached("create()") ReferenceEqualNode referenceEqualNode) {
            if (sameProfile.profile(referenceEqualNode.executeReferenceEqual(a, b))) {
                return true;
            } else {
                return areEqual(frame, a, b);
            }
        }

        private boolean areEqual(VirtualFrame frame, Object left, Object right) {
            if (equalNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                equalNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return equalNode.callBoolean(frame, left, "==", null, right);
        }

    }

    /** Check if operands are the same object or call #eql? */
    public abstract static class SameOrEqlNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode eqlNode;

        private final ConditionProfile sameProfile = ConditionProfile.createBinaryProfile();

        public abstract boolean executeSameOrEql(VirtualFrame frame, Object a, Object b);

        @Specialization
        public boolean sameOrEql(VirtualFrame frame, Object a, Object b,
                        @Cached("create()") ReferenceEqualNode referenceEqualNode) {
            if (sameProfile.profile(referenceEqualNode.executeReferenceEqual(a, b))) {
                return true;
            } else {
                return areEql(frame, a, b);
            }
        }

        private boolean areEql(VirtualFrame frame, Object left, Object right) {
            if (eqlNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                eqlNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }
            return eqlNode.callBoolean(frame, left, "eql?", null, right);
        }

    }

    @CoreMethod(names = { "<=>" }, required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        @Child private SameOrEqualNode equalNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = SameOrEqualNodeFactory.create(null);
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

    @CoreMethod(names = "binding", isModuleFunction = true)
    public abstract static class BindingNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject binding() {
            // Materialize the caller's frame - false means don't use a slow path to get it - we want to optimize it
            final MaterializedFrame callerFrame = getContext().getCallStack().getCallerFrameIgnoringSend()
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();

            return BindingNodes.createBinding(getContext(), callerFrame);
        }
    }

    @CoreMethod(names = "block_given?", isModuleFunction = true, needsCallerFrame = true)
    public abstract static class BlockGivenNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean blockGiven(MaterializedFrame callerFrame,
                                  @Cached("createBinaryProfile()") ConditionProfile blockProfile) {
            return blockProfile.profile(RubyArguments.getBlock(callerFrame) != null);
        }

        @TruffleBoundary
        @Specialization
        public boolean blockGiven(NotProvided noCallerFrame) {
            return RubyArguments.getBlock(Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_ONLY, false)) != null;
        }

    }

    @CoreMethod(names = "__callee__", needsSelf = false)
    public abstract static class CalleeNameNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject calleeName() {
            // the "called name" of a method.
            return getSymbol(getContext().getCallStack().getCallingMethodIgnoringSend().getName());
        }
    }

    @CoreMethod(names = "caller_locations", isModuleFunction = true, optional = 2, lowerFixnum = { 1, 2 })
    public abstract static class CallerLocationsNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject callerLocations(NotProvided omit, NotProvided length) {
            return callerLocations(1, -1);
        }

        @Specialization
        public DynamicObject callerLocations(int omit, NotProvided length) {
            return callerLocations(omit, -1);
        }

        @Specialization
        public DynamicObject callerLocations(int omit, int length) {
            final Backtrace backtrace = getContext().getCallStack().getBacktrace(this, 1 + omit, true, null);

            int locationsCount = backtrace.getActivations().size();

            if (length != -1 && locationsCount > length) {
                locationsCount = length;
            }

            final Object[] locations = new Object[locationsCount];

            for (int n = 0; n < locationsCount; n++) {
                Activation activation = backtrace.getActivations().get(n);
                locations[n] = Layouts.THREAD_BACKTRACE_LOCATION.createThreadBacktraceLocation(coreLibrary().getThreadBacktraceLocationFactory(), activation);
            }

            return createArray(locations, locations.length);
        }
    }

    @CoreMethod(names = "class")
    public abstract static class KernelClassNode extends CoreMethodArrayArgumentsNode {

        @Child private LogicalClassNode classNode;

        public KernelClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = LogicalClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject getClass(VirtualFrame frame, Object self) {
            return classNode.executeLogicalClass(self);
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
            final DynamicObject newObject = (DynamicObject) allocateNode.call(frame, rubyClass, "allocate");
            copyInstanceVariables(self, newObject);
            return newObject;
        }

        @TruffleBoundary
        private void copyInstanceVariables(DynamicObject from, DynamicObject to) {
            // Concurrency: OK if callers create the object and publish it after copy
            for (Property property : from.getShape().getProperties()) {
                if (property.getKey() instanceof String) {
                    to.define(property.getKey(), property.get(from, from.getShape()), 0);
                }
            }
        }

    }

    @CoreMethod(names = "clone", taintFrom = 0)
    public abstract static class CloneNode extends CoreMethodArrayArgumentsNode {

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
        public DynamicObject clone(VirtualFrame frame, DynamicObject self,
                @Cached("createBinaryProfile()") ConditionProfile isSingletonProfile,
                @Cached("createBinaryProfile()") ConditionProfile isFrozenProfile,
                @Cached("createBinaryProfile()") ConditionProfile isRubyClass) {
            final DynamicObject newObject = copyNode.executeCopy(frame, self);

            // Copy the singleton class if any.
            final DynamicObject selfMetaClass = Layouts.BASIC_OBJECT.getMetaClass(self);
            if (isSingletonProfile.profile(Layouts.CLASS.getIsSingleton(selfMetaClass))) {
                final DynamicObject newObjectMetaClass = singletonClassNode.executeSingletonClass(newObject);
                Layouts.MODULE.getFields(newObjectMetaClass).initCopy(selfMetaClass);
            }

            initializeCloneNode.call(frame, newObject, "initialize_clone", self);

            if (isFrozenProfile.profile(isFrozenNode.executeIsFrozen(self))) {
                freezeNode.executeFreeze(newObject);
            }

            if (isRubyClass.profile(RubyGuards.isRubyClass(self))) {
                Layouts.CLASS.setSuperclass(newObject, Layouts.CLASS.getSuperclass(self));
            }

            return newObject;
        }

    }

    @CoreMethod(names = "dup", taintFrom = 0)
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

            initializeDupNode.call(frame, newObject, "initialize_dup", self);

            return newObject;
        }

    }

    @CoreMethod(names = "eval", isModuleFunction = true, required = 1, optional = 3, lowerFixnum = 4)
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

        @CreateCast("source")
        public RubyNode coerceSourceToString(RubyNode source) {
            return ToStrNodeGen.create(null, null, source);
        }

        protected DynamicObject getCallerBinding(VirtualFrame frame) {
            if (bindingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                bindingNode = insert(KernelNodesFactory.BindingNodeFactory.create(null));
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
                "ropesEqual(source, cachedSource)",
                "!parseDependsOnDeclarationFrame(cachedRootNode)"
        }, limit = "getCacheLimit()")
        public Object evalNoBindingCached(
                VirtualFrame frame,
                DynamicObject source,
                NotProvided binding,
                NotProvided filename,
                NotProvided lineNumber,
                @Cached("privatizeRope(source)") Rope cachedSource,
                @Cached("compileSource(frame, source)") RootNodeWrapper cachedRootNode,
                @Cached("createCallTarget(cachedRootNode)") CallTarget cachedCallTarget,
                @Cached("create(cachedCallTarget)") DirectCallNode callNode
        ) {
            final DynamicObject callerBinding = getCallerBinding(frame);

            final MaterializedFrame parentFrame = Layouts.BINDING.getFrame(callerBinding);
            final Object callerSelf = RubyArguments.getSelf(frame);

            final InternalMethod method = new InternalMethod(
                    getContext(),
                    cachedRootNode.getRootNode().getSharedMethodInfo(),
                    cachedRootNode.getRootNode().getSharedMethodInfo().getName(),
                    RubyArguments.getMethod(parentFrame).getDeclaringModule(),
                    Visibility.PUBLIC,
                    cachedCallTarget);

            return callNode.call(frame, RubyArguments.pack(parentFrame, null, method, RubyArguments.getDeclarationContext(parentFrame), null, callerSelf, null, new Object[]{}));
        }

        @Specialization(guards = {
                "isRubyString(source)"
        }, contains = "evalNoBindingCached")
        public Object evalNoBindingUncached(VirtualFrame frame, DynamicObject source, NotProvided noBinding,
                                            NotProvided filename, NotProvided lineNumber, @Cached("create()") IndirectCallNode callNode) {
            final DynamicObject binding = getCallerBinding(frame);
            final MaterializedFrame topFrame = Layouts.BINDING.getFrame(binding);
            RubyArguments.setSelf(topFrame, RubyArguments.getSelf(frame));
            final CodeLoader.DeferredCall deferredCall = doEvalX(source, binding, "(eval)", 1, true);
            return deferredCall.call(frame, callNode);

        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isNil(noBinding)",
                "isRubyString(filename)"
        })
        public Object evalNilBinding(VirtualFrame frame, DynamicObject source, DynamicObject noBinding,
                                     DynamicObject filename, Object unusedLineNumber, @Cached("create()") IndirectCallNode callNode) {
            return evalNoBindingUncached(frame, source, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, callNode);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)"
        })
        public Object evalBinding(VirtualFrame frame, DynamicObject source, DynamicObject binding, NotProvided filename,
                                  NotProvided lineNumber, @Cached("create()") IndirectCallNode callNode) {
            final CodeLoader.DeferredCall deferredCall = doEvalX(source, binding, "(eval)", 1, false);
            return deferredCall.call(frame, callNode);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isNil(noFilename)",
                "isNil(noLineNumber)"
        })
        public Object evalBinding(VirtualFrame frame, DynamicObject source, DynamicObject binding, DynamicObject noFilename, DynamicObject noLineNumber, @Cached("create()") IndirectCallNode callNode) {
            return evalBinding(frame, source, binding, NotProvided.INSTANCE, NotProvided.INSTANCE, callNode);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isRubyString(filename)" })
        public Object evalBindingFilename(VirtualFrame frame, DynamicObject source, DynamicObject binding, DynamicObject filename,
                                          NotProvided lineNumber, @Cached("create()") IndirectCallNode callNode) {
            return evalBindingFilenameLine(frame, source, binding, filename, 0, callNode);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isRubyString(filename)",
                "isNil(noLineNumber)"
        })
        public Object evalBindingFilename(VirtualFrame frame, DynamicObject source, DynamicObject binding, DynamicObject filename, DynamicObject noLineNumber, @Cached("create()") IndirectCallNode callNode) {
            return evalBindingFilename(frame, source, binding, filename, NotProvided.INSTANCE, callNode);
        }

        @Specialization(guards = {
                "isRubyString(source)",
                "isRubyBinding(binding)",
                "isRubyString(filename)" })
        public Object evalBindingFilenameLine(VirtualFrame frame, DynamicObject source, DynamicObject binding, DynamicObject filename,
                                              int lineNumber, @Cached("create()") IndirectCallNode callNode) {
            final CodeLoader.DeferredCall deferredCall = doEvalX(source, binding, filename.toString(), lineNumber, false);
            return deferredCall.call(frame, callNode);
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyString(source)",
                "!isRubyBinding(badBinding)" })
        public Object evalBadBinding(DynamicObject source, DynamicObject badBinding, NotProvided filename,
                                     NotProvided lineNumber) {
            throw new RaiseException(coreExceptions().typeErrorWrongArgumentType(badBinding, "binding", this));
        }

        @TruffleBoundary
        private CodeLoader.DeferredCall doEvalX(DynamicObject rubySource,
                                                DynamicObject binding,
                                                String filename,
                                                int line,
                                                boolean ownScopeForAssignments) {
            final Rope code = StringOperations.rope(rubySource);

            // TODO (pitr 15-Oct-2015): fix this ugly hack, required for AS, copy-paste
            final String s = new String(new char[Math.max(line - 1, 0)]);
            final String space = StringUtils.replace(s, "\0", "\n");
            // TODO CS 14-Apr-15 concat space + code as a rope, otherwise the string will be copied after the rope is converted
            final Source source = getContext().getSourceLoader().loadFragment(space + RopeOperations.decodeRope(code), filename);

            final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
            final DeclarationContext declarationContext = RubyArguments.getDeclarationContext(frame);
            final RubyRootNode rootNode = getContext().getCodeLoader().parse(
                    source, code.getEncoding(), ParserContext.EVAL, frame, ownScopeForAssignments, this);
            return getContext().getCodeLoader().prepareExecute(
                    ParserContext.EVAL, declarationContext, rootNode, frame, RubyArguments.getSelf(frame));
        }

        protected RootNodeWrapper compileSource(VirtualFrame frame, DynamicObject sourceText) {
            assert RubyGuards.isRubyString(sourceText);

            final DynamicObject callerBinding = getCallerBinding(frame);
            final MaterializedFrame parentFrame = Layouts.BINDING.getFrame(callerBinding);

            final Encoding encoding = Layouts.STRING.getRope(sourceText).getEncoding();
            final Source source = getContext().getSourceLoader().loadFragment(sourceText.toString(), "(eval)");

            final TranslatorDriver translator = new TranslatorDriver(getContext());

            return new RootNodeWrapper(translator.parse(getContext(), source, encoding, ParserContext.EVAL, null, null, parentFrame, true, this));
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

    @CoreMethod(names = "exec", isModuleFunction = true, required = 1, rest = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class ExecNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toHashNode;

        @Specialization
        public Object exec(VirtualFrame frame, Object command, Object[] args) {
            if (TruffleOptions.AOT) {
                throw new UnsupportedOperationException("ProcessEnvironment.environment not supported with AOT.");
            }

            if (toHashNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toHashNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final String[] commandLine = buildCommandLine(command, args);

            final DynamicObject env = coreLibrary().getENV();
            final DynamicObject envAsHash = (DynamicObject) toHashNode.call(frame, env, "to_hash");

            exec(getContext(), envAsHash, commandLine);

            return null;
        }

        @TruffleBoundary
        private String[] buildCommandLine(Object command, Object[] args) {
            final List<String> commandLine = new ArrayList<>(1 + args.length);
            if (RubyGuards.isRubyArray(command)) {
                // For handling: exec([cmdname, argv0], arg1, ...)
                // argv0 not yet implemented
                final Object[] store = (Object[]) getStore((DynamicObject) command);
                commandLine.add(store[0].toString());
            } else {
                commandLine.add(command.toString());
            }
            for (int n = 0; n < args.length; n++) {
                if (n == args.length - 1 && RubyGuards.isRubyHash(args[n])) {
                    break;
                }
                commandLine.add(args[n].toString());
            }
            final String[] result = new String[commandLine.size()];
            return commandLine.toArray(result);
        }

        @TruffleBoundary
        private void exec(RubyContext context, DynamicObject envAsHash, String[] commandLine) {
            final ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.inheritIO();

            for (KeyValue keyValue : HashOperations.iterableKeyValues(envAsHash)) {
                builder.environment().put(keyValue.getKey().toString(), keyValue.getValue().toString());
            }

            final Process process;

            try {
                process = builder.start();
            } catch (IOException e) {
                if (e.getMessage().contains("Permission denied")) {
                    throw new RaiseException(getContext().getCoreExceptions().errnoError(Errno.EACCES.intValue(), this));
                } else if (e.getMessage().contains("No such file or directory")) {
                    throw new RaiseException(getContext().getCoreExceptions().errnoError(Errno.ENOENT.intValue(), this));
                } else {
                    // TODO(cs): proper Ruby exception
                    throw new JavaException(e);
                }
            }

            int exitCode = context.getThreadManager().runUntilResult(this, () -> process.waitFor());

            /*
             * We really do want to just exit here as opposed to throwing a MainExitException and tidying up, as we're
             * pretending that we did exec and so replaced this process with a new one.
             */

            System.exit(exitCode);
        }

    }

    @CoreMethod(names = "freeze")
    public abstract static class KernelFreezeNode extends CoreMethodArrayArgumentsNode {

        @Child private FreezeNode freezeNode;

        public KernelFreezeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            freezeNode = FreezeNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public Object freeze(Object self) {
            return freezeNode.executeFreeze(self);
        }

    }

    @CoreMethod(names = "frozen?")
    public abstract static class KernelFrozenNode extends CoreMethodArrayArgumentsNode {

        @Child private IsFrozenNode isFrozenNode;

        @Specialization
        public boolean isFrozen(Object self) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), null, null));
            }

            return isFrozenNode.executeIsFrozen(self);
        }

    }

    @CoreMethod(names = "gets", isModuleFunction = true, unsafe = UnsafeGroup.IO)
    public abstract static class GetsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject gets() {
            // TODO(CS): having some trouble interacting with JRuby stdin - so using this hack
            final InputStream in = getContext().getInstanceConfig().getInput();

            Encoding encoding = getContext().getEncodingManager().getDefaultExternalEncoding();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding.getCharset()));

            final String line = getContext().getThreadManager().runUntilResult(this, () -> gets(reader));

            final DynamicObject rubyLine = createString(StringOperations.encodeRope(line, UTF8Encoding.INSTANCE));

            // Set the local variable $_ in the caller

            final Frame caller = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.READ_WRITE, true);

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
                throw new JavaException(e);
            }
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        private static final int MURMUR_SEED = System.identityHashCode(HashNode.class);

        @Specialization
        public long hash(int value) {
            return Hashing.hash(MURMUR_SEED, value);
        }

        @Specialization
        public long hash(long value) {
            return Hashing.hash(MURMUR_SEED, value);
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

        private final BranchProfile errorProfile = BranchProfile.create();

        @Specialization
        public Object initializeCopy(DynamicObject self, DynamicObject from) {
            if (Layouts.BASIC_OBJECT.getLogicalClass(self) != Layouts.BASIC_OBJECT.getLogicalClass(from)) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeError("initialize_copy should take same class object", this));
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
            return initializeCopyNode.call(frame, self, "initialize_copy", from);
        }

    }

    @CoreMethod(names = "instance_of?", required = 1)
    public abstract static class InstanceOfNode extends CoreMethodArrayArgumentsNode {

        @Child private LogicalClassNode classNode;

        public InstanceOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = LogicalClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization(guards = "isRubyModule(rubyClass)")
        public boolean instanceOf(VirtualFrame frame, Object self, DynamicObject rubyClass) {
            return classNode.executeLogicalClass(self) == rubyClass;
        }

    }

    @CoreMethod(names = "instance_variable_defined?", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class InstanceVariableDefinedNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary
        @Specialization
        public boolean isInstanceVariableDefined(DynamicObject object, String name) {
            final String ivar = SymbolTable.checkInstanceVariableName(getContext(), name, object, this);
            final Property property = object.getShape().getProperty(ivar);
            return PropertyFlags.isDefined(property);
        }

    }

    @CoreMethod(names = "instance_variable_get", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class InstanceVariableGetNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceName(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        public Object instanceVariableGetSymbol(DynamicObject object, String name,
                @Cached("createObjectIVarGetNode()") ObjectIVarGetNode iVarGetNode) {
            return iVarGetNode.executeIVarGet(object, name);
        }

        protected ObjectIVarGetNode createObjectIVarGetNode() {
            return ObjectIVarGetNodeGen.create(true, null, null);
        }

    }

    @CoreMethod(names = { "instance_variable_set", "__instance_variable_set__" }, raiseIfFrozenSelf = true, required = 2)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "name"),
            @NodeChild(type = RubyNode.class, value = "value")
    })
    public abstract static class InstanceVariableSetNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceName(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization
        public Object instanceVariableSet(DynamicObject object, String name, Object value,
                @Cached("createObjectIVarSetNode()") ObjectIVarSetNode iVarSetNode) {
            return iVarSetNode.executeIVarSet(object, name, value);
        }

        protected ObjectIVarSetNode createObjectIVarSetNode() {
            return ObjectIVarSetNodeGen.create(true, null, null, null);
        }

    }

    @CoreMethod(names = "remove_instance_variable", raiseIfFrozenSelf = true, required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class RemoveInstanceVariableNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary
        @Specialization
        public Object removeInstanceVariable(DynamicObject object, String name) {
            final String ivar = SymbolTable.checkInstanceVariableName(getContext(), name, object, this);
            final Object value = object.get(ivar, nil());

            if (SharedObjects.isShared(object)) {
                synchronized (object) {
                    removeField(object, name);
                }
            } else {
                if (!object.delete(name)) {
                    throw new RaiseException(coreExceptions().nameErrorInstanceVariableNotDefined(name, object, this));
                }
            }
            return value;
        }

        private void removeField(DynamicObject object, String name) {
            Shape shape = object.getShape();
            Property property = shape.getProperty(name);
            if (!PropertyFlags.isDefined(property)) {
                throw new RaiseException(coreExceptions().nameErrorInstanceVariableNotDefined(name, object, this));
            }

            Shape newShape = shape.replaceProperty(property, PropertyFlags.asRemoved(property));
            object.setShapeAndGrow(shape, newShape);
        }
    }

    @CoreMethod(names = "instance_variables")
    public abstract static class InstanceVariablesNode extends CoreMethodArrayArgumentsNode {

        @Child private BasicObjectNodes.InstanceVariablesNode instanceVariablesNode;

        public InstanceVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            instanceVariablesNode = BasicObjectNodesFactory.InstanceVariablesNodeFactory.create(new RubyNode[] {});
        }

        @Specialization
        public DynamicObject instanceVariables(VirtualFrame frame, Object self) {
            return instanceVariablesNode.execute(self);
        }

    }

    @CoreMethod(names = { "is_a?", "kind_of?" }, required = 1)
    public abstract static class KernelIsANode extends CoreMethodArrayArgumentsNode {

        @Child IsANode isANode;

        public KernelIsANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = IsANodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public boolean isA(Object self, DynamicObject module) {
            return isANode.executeIsA(self, module);
        }

        @Specialization(guards = "!isRubyModule(module)")
        public boolean isATypeError(Object self, Object module) {
            throw new RaiseException(coreExceptions().typeError("class or module required", this));
        }

    }

    @CoreMethod(names = "lambda", isModuleFunction = true, needsBlock = true)
    public abstract static class LambdaNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject lambda(NotProvided block) {
            final Frame parentFrame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameAccess.READ_ONLY, true);
            final DynamicObject parentBlock = RubyArguments.getBlock(parentFrame);

            if (parentBlock == null) {
                throw new RaiseException(coreExceptions().argumentError("tried to create Proc object without a block", this));
            }

            return lambda(parentBlock);
        }

        @Specialization
        public DynamicObject lambda(DynamicObject block) {
            return ProcOperations.createRubyProc(
                    coreLibrary().getProcFactory(),
                    ProcType.LAMBDA,
                    Layouts.PROC.getSharedMethodInfo(block),
                    Layouts.PROC.getCallTargetForLambdas(block),
                    Layouts.PROC.getCallTargetForLambdas(block),
                    Layouts.PROC.getDeclarationFrame(block),
                    Layouts.PROC.getMethod(block),
                    Layouts.PROC.getSelf(block),
                    Layouts.PROC.getBlock(block));
        }
    }

    @CoreMethod(names = "local_variables", needsSelf = false)
    public abstract static class LocalVariablesNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject localVariables() {
            final Frame frame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.READ_ONLY, true);
            return BindingNodes.LocalVariablesNode.listLocalVariables(getContext(), frame);
        }

    }

    @CoreMethod(names = "__method__", needsSelf = false)
    public abstract static class MethodNameNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject methodName() {
            // the "original/definition name" of the method.
            return getSymbol(getContext().getCallStack().getCallingMethodIgnoringSend().getSharedMethodInfo().getName());
        }

    }

    @CoreMethod(names = "method", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class MethodNode extends CoreMethodNode {

        @Child NameToJavaStringNode nameToJavaStringNode;
        @Child LookupMethodNode lookupMethodNode;
        @Child CallDispatchHeadNode respondToMissingNode;

        public MethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            nameToJavaStringNode = NameToJavaStringNode.create();
            lookupMethodNode = LookupMethodNodeGen.create(context, sourceSection, true, false, null, null);
            respondToMissingNode = DispatchHeadNodeFactory.createMethodCall(getContext(), true);
        }

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToSymbolOrStringNodeGen.create(null, null, name);
        }

        @Specialization
        public DynamicObject method(VirtualFrame frame, Object self, DynamicObject name,
                @Cached("createBinaryProfile()") ConditionProfile notFoundProfile,
                @Cached("createBinaryProfile()") ConditionProfile respondToMissingProfile) {
            final String normalizedName = nameToJavaStringNode.executeToJavaString(frame, name);
            InternalMethod method = lookupMethodNode.executeLookupMethod(frame, self, normalizedName);

            if (notFoundProfile.profile(method == null)) {
                if (respondToMissingProfile.profile(respondToMissingNode.callBoolean(frame, self, "respond_to_missing?", null, name, true))) {
                    final InternalMethod methodMissing = lookupMethodNode.executeLookupMethod(frame, self, "method_missing").withName(normalizedName);
                    method = createMissingMethod(self, name, normalizedName, methodMissing);
                } else {
                    throw new RaiseException(coreExceptions().nameErrorUndefinedMethod(normalizedName, coreLibrary().getLogicalClass(self), this));
                }
            }

            return Layouts.METHOD.createMethod(coreLibrary().getMethodFactory(), self, method);
        }

        @TruffleBoundary
        private InternalMethod createMissingMethod(Object self, DynamicObject name, String normalizedName, InternalMethod methodMissing) {
            final SharedMethodInfo info = methodMissing.getSharedMethodInfo().withName(normalizedName);

            final RubyNode newBody = new CallMethodMissingWithStaticName(getContext(), info.getSourceSection(), name);
            final RubyRootNode newRootNode = new RubyRootNode(getContext(), info.getSourceSection(), new FrameDescriptor(nil()), info, newBody, false);
            final CallTarget newCallTarget = Truffle.getRuntime().createCallTarget(newRootNode);

            final DynamicObject module = coreLibrary().getMetaClass(self);
            return new InternalMethod(getContext(), info, normalizedName, module, Visibility.PUBLIC, newCallTarget);
        }

        private static class CallMethodMissingWithStaticName extends RubyNode {

            private final DynamicObject methodName;
            @Child private CallDispatchHeadNode methodMissing;

            public CallMethodMissingWithStaticName(RubyContext context, SourceSection sourceSection, DynamicObject methodName) {
                super(context, sourceSection);
                this.methodName = methodName;
                methodMissing = DispatchHeadNodeFactory.createMethodCall(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                final Object[] originalUserArguments = RubyArguments.getArguments(frame);
                final Object[] newUserArguments = ArrayUtils.unshift(originalUserArguments, methodName);
                return methodMissing.callWithBlock(frame, RubyArguments.getSelf(frame), "method_missing", RubyArguments.getBlock(frame), newUserArguments);
            }
        }

    }

    @CoreMethod(names = "methods", optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "object"),
            @NodeChild(type = RubyNode.class, value = "regular")
    })
    public abstract static class MethodsNode extends CoreMethodNode {

        @CreateCast("regular")
        public RubyNode coerceToBoolean(RubyNode regular) {
            return BooleanCastWithDefaultNodeGen.create(true, regular);
        }

        @TruffleBoundary
        @Specialization(guards = "regular")
        public DynamicObject methodsRegular(Object self, boolean regular,
                                            @Cached("createMetaClassNode()") MetaClassNode metaClassNode) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(self);

            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(getContext(), regular, MethodFilter.PUBLIC_PROTECTED).toArray();
            return createArray(objects, objects.length);
        }

        @Specialization(guards = "!regular")
        public DynamicObject methodsSingleton(VirtualFrame frame, Object self, boolean regular,
                                              @Cached("createSingletonMethodsNode()") SingletonMethodsNode singletonMethodsNode) {
            return singletonMethodsNode.executeSingletonMethods(frame, self, false);
        }

        protected MetaClassNode createMetaClassNode() {
            return MetaClassNodeGen.create(getContext(), null, null);
        }

        protected SingletonMethodsNode createSingletonMethodsNode() {
            return SingletonMethodsNodeFactory.create(getContext(), null, null, null);
        }

    }

    @CoreMethod(names = "nil?", needsSelf = false)
    public abstract static class NilNode extends CoreMethodArrayArgumentsNode {

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
            return BooleanCastWithDefaultNodeGen.create(true, includeAncestors);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject privateMethods(Object self, boolean includeAncestors) {
            DynamicObject metaClass = metaClassNode.executeMetaClass(self);

            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(getContext(), includeAncestors, MethodFilter.PRIVATE).toArray();
            return createArray(objects, objects.length);
        }

    }

    @CoreMethod(names = "proc", isModuleFunction = true, needsBlock = true)
    public abstract static class ProcNode extends CoreMethodArrayArgumentsNode {

        @Child ProcNewNode procNewNode;

        public ProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            procNewNode = ProcNewNodeFactory.create(null);
        }

        @Specialization
        public DynamicObject proc(VirtualFrame frame, Object maybeBlock) {
            return procNewNode.executeProcNew(frame, coreLibrary().getProcClass(), ArrayUtils.EMPTY_ARRAY, maybeBlock);
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
            return BooleanCastWithDefaultNodeGen.create(true, includeAncestors);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject protectedMethods(Object self, boolean includeAncestors) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(self);

            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(getContext(), includeAncestors, MethodFilter.PROTECTED).toArray();
            return createArray(objects, objects.length);
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
            return BooleanCastWithDefaultNodeGen.create(true, includeAncestors);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject publicMethods(Object self, boolean includeAncestors) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(self);

            Object[] objects = Layouts.MODULE.getFields(metaClass).filterMethodsOnObject(getContext(), includeAncestors, MethodFilter.PUBLIC).toArray();
            return createArray(objects, objects.length);
        }

    }

    @CoreMethod(names = "public_send", needsBlock = true, required = 1, rest = true)
    public abstract static class PublicSendNode extends CoreMethodArrayArgumentsNode {

        @Child private DispatchHeadNode dispatchNode;

        public PublicSendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatchNode = new DispatchHeadNode(context, false, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, NotProvided block) {
            return send(frame, self, name, args, (DynamicObject) null);
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, DynamicObject block) {
            return dispatchNode.dispatch(frame, self, name, block, args);
        }

    }

    @CoreMethod(names = "require", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.LOAD)
    @NodeChild(type = RubyNode.class, value = "feature")
    public abstract static class KernelRequireNode extends CoreMethodNode {

        @CreateCast("feature")
        public RubyNode coerceFeatureToPath(RubyNode feature) {
            return ToPathNodeGen.create(null, null, feature);
        }

        @Specialization(guards = "isRubyString(featureString)")
        public boolean require(VirtualFrame frame, DynamicObject featureString,
                @Cached("create()") RequireNode requireNode) {

            String feature = StringOperations.getString(featureString);

            // Pysch loads either the jar or the so - we need to intercept
            if (feature.equals("psych.so") && callerIs("mri/psych.rb")) {
                feature = "truffle/psych.rb";
            }

            // TODO CS 1-Mar-15 ERB will use strscan if it's there, but strscan is not yet complete, so we need to hide it
            if (feature.equals("strscan") && callerIs("mri/erb.rb")) {
                throw new RaiseException(coreExceptions().loadErrorCannotLoad(feature, this));
            }

            return requireNode.executeRequire(frame, feature);
        }

        @TruffleBoundary
        private boolean callerIs(String caller) {
            for (Activation activation : getContext().getCallStack().getBacktrace(this).getActivations()) {

                final Source source = activation.getCallNode().getEncapsulatingSourceSection().getSource();

                if (source != null && source.getName().endsWith(caller)) {
                    return true;
                }
            }

            return false;
        }
    }

    @CoreMethod(names = "require_relative", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.LOAD)
    public abstract static class RequireRelativeNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(feature)")
        public boolean requireRelative(VirtualFrame frame, DynamicObject feature,
                @Cached("create()") RequireNode requireNode) {
            final String featureString = StringOperations.getString(feature);
            final String featurePath = getFullPath(featureString);

            return requireNode.executeRequire(frame, featurePath);
        }

        @TruffleBoundary
        private String getFullPath(final String featureString) {
            final String featurePath;

            if (featureString.startsWith(SourceLoader.TRUFFLE_SCHEME) || featureString.startsWith(SourceLoader.JRUBY_SCHEME) || new File(featureString).isAbsolute()) {
                featurePath = featureString;
            } else {
                final Source source = getContext().getCallStack().getCallerFrameIgnoringSend().getCallNode().getEncapsulatingSourceSection().getSource();

                String sourcePath = source.getPath();
                if (sourcePath == null) {
                    // Use the filename passed to eval as basepath
                    sourcePath = source.getName();
                }

                if (sourcePath == null) {
                    throw new RaiseException(coreExceptions().loadError("cannot infer basepath", featureString, this));
                }

                featurePath = dirname(sourcePath) + "/" + featureString;
            }
            return featurePath;
        }

        private String dirname(String path) {
            int lastSlash = path.lastIndexOf(File.separatorChar);
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
        @Child private DoesRespondDispatchHeadNode dispatchRespondToMissing;
        @Child private CallDispatchHeadNode respondToMissingNode;
        private final ConditionProfile ignoreVisibilityProfile = ConditionProfile.createBinaryProfile();

        public RespondToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatch = new DoesRespondDispatchHeadNode(context, false);
            dispatchIgnoreVisibility = new DoesRespondDispatchHeadNode(context, true);
            dispatchRespondToMissing = new DoesRespondDispatchHeadNode(context, true);
        }

        public abstract boolean executeDoesRespondTo(VirtualFrame frame, Object object, Object name, boolean includeProtectedAndPrivate);

        @CreateCast("includeProtectedAndPrivate")
        public RubyNode coerceToBoolean(RubyNode includeProtectedAndPrivate) {
            return BooleanCastWithDefaultNodeGen.create(false, includeProtectedAndPrivate);
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
            } else if (dispatchRespondToMissing.doesRespondTo(frame, "respond_to_missing?", object)) {
                return respondToMissing(frame, object, getSymbol(StringOperations.rope(name)), includeProtectedAndPrivate);
            } else {
                return false;
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
            } else if (dispatchRespondToMissing.doesRespondTo(frame, "respond_to_missing?", object)) {
                return respondToMissing(frame, object, name, includeProtectedAndPrivate);
            } else {
                return false;
            }
        }

        private boolean respondToMissing(VirtualFrame frame, Object object, DynamicObject name, boolean includeProtectedAndPrivate) {
            if (respondToMissingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                respondToMissingNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return respondToMissingNode.callBoolean(frame, object, "respond_to_missing?", null, name, includeProtectedAndPrivate);
        }
    }

    @CoreMethod(names = "respond_to_missing?", required = 2)
    public abstract static class RespondToMissingNode extends CoreMethodArrayArgumentsNode {

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

        @Specialization(guards = "isNil(nil)")
        public DynamicObject setTraceFunc(Object nil) {
            getContext().getTraceManager().setTraceFunc(null);
            return nil();
        }

        @Specialization(guards = "isRubyProc(traceFunc)")
        public DynamicObject setTraceFunc(DynamicObject traceFunc) {
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
        public DynamicObject singletonClass(Object self) {
            return singletonClassNode.executeSingletonClass(self);
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
            return BooleanCastWithDefaultNodeGen.create(true, includeAncestors);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject singletonMethods(Object self, boolean includeAncestors) {
            final DynamicObject metaClass = metaClassNode.executeMetaClass(self);

            if (!Layouts.CLASS.getIsSingleton(metaClass)) {
                return createArray(null, 0);
            }

            Object[] objects = Layouts.MODULE.getFields(metaClass).filterSingletonMethods(getContext(), includeAncestors, MethodFilter.PUBLIC_PROTECTED).toArray();
            return createArray(objects, objects.length);
        }

    }

    @NodeChild(value = "duration", type = RubyNode.class)
    @CoreMethod(names = "sleep", isModuleFunction = true, optional = 1)
    public abstract static class SleepNode extends CoreMethodNode {

        @CreateCast("duration")
        public RubyNode coerceDuration(RubyNode duration) {
            return DurationToMillisecondsNodeGen.create(false, duration);
        }

        @Specialization
        public long sleep(long duration) {
            return doSleepMillis(duration);
        }

        @TruffleBoundary
        private long doSleepMillis(final long durationInMillis) {
            if (durationInMillis < 0) {
                throw new RaiseException(coreExceptions().argumentError("time interval must be positive", this));
            }

            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();

            // Clear the wakeUp flag, following Ruby semantics:
            // it should only be considered if we are inside the sleep when Thread#{run,wakeup} is called.
            Layouts.THREAD.getWakeUp(thread).set(false);

            return sleepFor(this, getContext(), durationInMillis);
        }

        public static long sleepFor(Node currentNode, RubyContext context, final long durationInMillis) {
            assert durationInMillis >= 0;

            final DynamicObject thread = context.getThreadManager().getCurrentThread();

            final long start = System.currentTimeMillis();

            long slept = context.getThreadManager().runUntilResult(currentNode, () -> {
                long now = System.currentTimeMillis();
                long slept1 = now - start;

                if (slept1 >= durationInMillis || Layouts.THREAD.getWakeUp(thread).getAndSet(false)) {
                    return slept1;
                }
                Thread.sleep(durationInMillis - slept1);

                return System.currentTimeMillis() - start;
            });

            return slept / 1000;
        }

    }

    @CoreMethod(names = { "format", "sprintf" }, isModuleFunction = true, rest = true, required = 1, taintFrom = 1)
    @ImportStatic(StringCachingGuards.class)
    public abstract static class SprintfNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;
        @Child private TaintNode taintNode;

        private final BranchProfile exceptionProfile = BranchProfile.create();
        private final ConditionProfile resizeProfile = ConditionProfile.createBinaryProfile();

        @Specialization(guards = { "isRubyString(format)", "ropesEqual(format, cachedFormat)" })
        public DynamicObject formatCached(
                VirtualFrame frame,
                DynamicObject format,
                Object[] arguments,
                @Cached("privatizeRope(format)") Rope cachedFormat,
                @Cached("ropeLength(cachedFormat)") int cachedFormatLength,
                @Cached("create(compileFormat(format))") DirectCallNode callPackNode) {
            final BytesResult result;

            try {
                result = (BytesResult) callPackNode.call(frame,
                        new Object[]{ arguments, arguments.length });
            } catch (FormatException e) {
                exceptionProfile.enter();
                throw FormatExceptionTranslator.translate(this, e);
            }

            return finishFormat(cachedFormatLength, result);
        }

        @Specialization(guards = "isRubyString(format)", contains = "formatCached")
        public DynamicObject formatUncached(
                VirtualFrame frame,
                DynamicObject format,
                Object[] arguments,
                @Cached("create()") IndirectCallNode callPackNode) {
            final BytesResult result;

            try {
                result = (BytesResult) callPackNode.call(frame, compileFormat(format),
                        new Object[]{ arguments, arguments.length });
            } catch (FormatException e) {
                exceptionProfile.enter();
                throw FormatExceptionTranslator.translate(this, e);
            }

            return finishFormat(Layouts.STRING.getRope(format).byteLength(), result);
        }

        private DynamicObject finishFormat(int formatLength, BytesResult result) {
            byte[] bytes = result.getOutput();

            if (resizeProfile.profile(bytes.length != result.getOutputLength())) {
                bytes = Arrays.copyOf(bytes, result.getOutputLength());
            }

            if (makeLeafRopeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                makeLeafRopeNode = insert(RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null));
            }

            final DynamicObject string = createString(makeLeafRopeNode.executeMake(
                    bytes,
                    result.getEncoding().getEncodingForLength(formatLength),
                    result.getStringCodeRange(),
                    result.getOutputLength()));

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    taintNode = insert(TaintNodeGen.create(getContext(), null, null));
                }

                taintNode.executeTaint(string);
            }

            return string;
        }

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            assert RubyGuards.isRubyString(format);

            try {
                return new PrintfCompiler(getContext(), this)
                        .compile(Layouts.STRING.getRope(format).getBytes());
            } catch (InvalidFormatException e) {
                throw new RaiseException(coreExceptions().argumentError(e.getMessage(), this));
            }
        }

    }

    @Primitive(name = "kernel_global_variables")
    public abstract static class KernelGlobalVariablesPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject globalVariables() {
            final Collection<String> keys = coreLibrary().getGlobalVariables().keys();
            final Object[] store = new Object[keys.size()];
            int i = 0;
            for (String key : keys) {
                store[i] = getSymbol(key);
                i++;
            }
            return createArray(store, store.length);
        }

    }

    @CoreMethod(names = "taint")
    public abstract static class KernelTaintNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintNode taintNode;

        @Specialization
        public Object taint(Object object) {
            if (taintNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                taintNode = insert(TaintNodeGen.create(getContext(), null, null));
            }
            return taintNode.executeTaint(object);
        }

    }

    @CoreMethod(names = "tainted?")
    public abstract static class KernelIsTaintedNode extends CoreMethodArrayArgumentsNode {

        @Child private IsTaintedNode isTaintedNode;

        @Specialization
        public boolean isTainted(Object object) {
            if (isTaintedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isTaintedNode = insert(IsTaintedNodeGen.create(getContext(), null, null));
            }
            return isTaintedNode.executeIsTainted(object);
        }

    }

    public abstract static class ToHexStringNode extends CoreMethodArrayArgumentsNode {

        public abstract String executeToHexString(Object value);

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

        @Child private LogicalClassNode classNode;
        @Child private ObjectNodes.ObjectIDPrimitiveNode objectIDNode;
        @Child private TaintResultNode taintResultNode;
        @Child private ToHexStringNode toHexStringNode;

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = LogicalClassNodeGen.create(context, sourceSection, null);
            objectIDNode = ObjectNodesFactory.ObjectIDPrimitiveNodeFactory.create(null);
            taintResultNode = new TaintResultNode(context, sourceSection);
            toHexStringNode = KernelNodesFactory.ToHexStringNodeFactory.create(null);
        }

        public abstract DynamicObject executeToS(Object self);

        @Specialization
        public DynamicObject toS(Object self) {
            String className = Layouts.MODULE.getFields(classNode.executeLogicalClass(self)).getName();
            Object id = objectIDNode.executeObjectID(self);
            String hexID = toHexStringNode.executeToHexString(id);

            final DynamicObject string = createString(formatToS(className, hexID));
            taintResultNode.maybeTaint(self, string);
            return string;
        }

        @TruffleBoundary
        private Rope formatToS(String className, String hexID) {
            return StringOperations.encodeRope("#<" + className + ":0x" + hexID + ">", UTF8Encoding.INSTANCE);
        }

    }

    @CoreMethod(names = "untaint")
    public abstract static class UntaintNode extends CoreMethodArrayArgumentsNode {

        @Child private IsFrozenNode isFrozenNode;
        @Child private IsTaintedNode isTaintedNode;
        @Child private WriteObjectFieldNode writeTaintNode;

        public UntaintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isTaintedNode = IsTaintedNodeGen.create(context, sourceSection, null);
            writeTaintNode = WriteObjectFieldNodeGen.create(Layouts.TAINTED_IDENTIFIER);
        }

        @Specialization
        public int untaint(int num) {
            return num;
        }

        @Specialization
        public long untaint(long num) {
            return num;
        }

        @Specialization
        public double untaint(double num) {
            return num;
        }

        @Specialization
        public boolean untaint(boolean bool) {
            return bool;
        }

        @Specialization
        public Object taint(DynamicObject object) {
            if (!isTaintedNode.executeIsTainted(object)) {
                return object;
            }

            checkFrozen(object);
            writeTaintNode.execute(object, false);
            return object;
        }

        protected void checkFrozen(Object object) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), null, null));
            }
            isFrozenNode.raiseIfFrozen(object);
        }

    }

}
