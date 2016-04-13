/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.method;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.basicobject.BasicObjectNodes.ReferenceEqualNode;
import org.jruby.truffle.core.basicobject.BasicObjectNodesFactory;
import org.jruby.truffle.core.cast.ProcOrNullNode;
import org.jruby.truffle.core.cast.ProcOrNullNodeGen;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.ArgumentDescriptorUtils;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.CallBoundMethodNode;
import org.jruby.truffle.language.methods.CallBoundMethodNodeGen;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.LogicalClassNode;
import org.jruby.truffle.language.objects.LogicalClassNodeGen;

@CoreClass(name = "Method")
public abstract class MethodNodes {

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child protected ReferenceEqualNode referenceEqualNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        protected boolean areSame(VirtualFrame frame, Object left, Object right) {
            if (referenceEqualNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                referenceEqualNode = insert(BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(getContext(), getSourceSection(), null, null));
            }
            return referenceEqualNode.executeReferenceEqual(frame, left, right);
        }

        @Specialization(guards = "isRubyMethod(b)")
        public boolean equal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return areSame(frame, Layouts.METHOD.getReceiver(a), Layouts.METHOD.getReceiver(b)) && Layouts.METHOD.getMethod(a) == Layouts.METHOD.getMethod(b);
        }

        @Specialization(guards = "!isRubyMethod(b)")
        public boolean equal(DynamicObject a, Object b) {
            return false;
        }

    }

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int arity(DynamicObject method) {
            return Layouts.METHOD.getMethod(method).getSharedMethodInfo().getArity().getArityNumber();
        }

    }

    @CoreMethod(names = { "call", "[]" }, needsBlock = true, rest = true)
    public abstract static class CallNode extends CoreMethodArrayArgumentsNode {

        @Child CallBoundMethodNode callBoundMethodNode;
        @Child ProcOrNullNode procOrNullNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            callBoundMethodNode = CallBoundMethodNodeGen.create(context, sourceSection, null, null, null);
            procOrNullNode = ProcOrNullNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        protected Object call(VirtualFrame frame, DynamicObject method, Object[] arguments, Object block) {
            return callBoundMethodNode.executeCallBoundMethod(frame, method, arguments, procOrNullNode.executeProcOrNull(block));
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject name(DynamicObject method) {
            CompilerDirectives.transferToInterpreter();

            return getSymbol(Layouts.METHOD.getMethod(method).getName());
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject owner(DynamicObject method) {
            return Layouts.METHOD.getMethod(method).getDeclaringModule();
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject parameters(DynamicObject method) {
            final ArgumentDescriptor[] argsDesc = Layouts.METHOD.getMethod(method).getSharedMethodInfo().getArgumentDescriptors();

            return ArgumentDescriptorUtils.argumentDescriptorsToParameters(getContext(), argsDesc, true);
        }

    }

    @CoreMethod(names = "receiver")
    public abstract static class ReceiverNode extends CoreMethodArrayArgumentsNode {

        public ReceiverNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object receiver(DynamicObject method) {
            return Layouts.METHOD.getReceiver(method);
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object sourceLocation(DynamicObject method) {
            CompilerDirectives.transferToInterpreter();

            SourceSection sourceSection = Layouts.METHOD.getMethod(method).getSharedMethodInfo().getSourceSection();

            if (sourceSection.getSource() == null) {
                return nil();
            } else {
                DynamicObject file = createString(StringOperations.encodeRope(sourceSection.getSource().getName(), UTF8Encoding.INSTANCE));
                Object[] objects = new Object[]{file, sourceSection.getStartLine()};
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), objects, objects.length);
            }
        }

    }

    @CoreMethod(names = "unbind")
    public abstract static class UnbindNode extends CoreMethodArrayArgumentsNode {

        @Child private LogicalClassNode classNode;

        public UnbindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = LogicalClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject unbind(VirtualFrame frame, DynamicObject method) {
            final DynamicObject receiverClass = classNode.executeLogicalClass(Layouts.METHOD.getReceiver(method));
            return Layouts.UNBOUND_METHOD.createUnboundMethod(coreLibrary().getUnboundMethodFactory(), receiverClass, Layouts.METHOD.getMethod(method));
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "methodObject == cachedMethodObject", limit = "getCacheLimit()")
        public DynamicObject toProcCached(DynamicObject methodObject,
                @Cached("methodObject") DynamicObject cachedMethodObject,
                @Cached("toProcUncached(cachedMethodObject)") DynamicObject proc) {
            return proc;
        }

        @Specialization
        public DynamicObject toProcUncached(DynamicObject methodObject) {
            final CallTarget callTarget = method2proc(methodObject);
            final InternalMethod method = Layouts.METHOD.getMethod(methodObject);

            return ProcOperations.createRubyProc(
                    coreLibrary().getProcFactory(),
                    ProcType.LAMBDA,
                    method.getSharedMethodInfo(),
                    callTarget,
                    callTarget,
                    null,
                    method,
                    Layouts.METHOD.getReceiver(methodObject),
                    null);
        }

        @TruffleBoundary
        protected CallTarget method2proc(DynamicObject methodObject) {
            // translate to something like:
            // lambda { |same args list| method.call(args) }
            // We need to preserve the method receiver and we want to have the same argument list

            final InternalMethod method = Layouts.METHOD.getMethod(methodObject);
            final SourceSection sourceSection = method.getSharedMethodInfo().getSourceSection();
            final RootNode oldRootNode = ((RootCallTarget) method.getCallTarget()).getRootNode();

            final SetReceiverNode setReceiverNode = new SetReceiverNode(getContext(), sourceSection, Layouts.METHOD.getReceiver(methodObject), method.getCallTarget());
            final RootNode newRootNode = new RubyRootNode(getContext(), sourceSection, oldRootNode.getFrameDescriptor(), method.getSharedMethodInfo(), setReceiverNode, false);
            return Truffle.getRuntime().createCallTarget(newRootNode);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().METHOD_TO_PROC_CACHE;
        }

    }

    private static class SetReceiverNode extends RubyNode {

        private final Object receiver;
        @Child private DirectCallNode methodCallNode;

        public SetReceiverNode(RubyContext context, SourceSection sourceSection, Object receiver, CallTarget methodCallTarget) {
            super(context, sourceSection);
            this.receiver = receiver;
            this.methodCallNode = DirectCallNode.create(methodCallTarget);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            RubyArguments.setSelf(frame.getArguments(), receiver);
            return methodCallNode.call(frame, frame.getArguments());
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
