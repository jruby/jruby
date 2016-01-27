/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.dispatch.RubyCallNode;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.nodes.supercall.SuperCallNode;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.ArrayOperations;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.Arrays;
import java.util.List;

@CoreClass(name = "BasicObject")
public abstract class BasicObjectNodes {

    @CoreMethod(names = "!")
    public abstract static class NotNode extends UnaryCoreMethodNode {

        public NotNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("operand") public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeGen.create(getContext(), getSourceSection(), operand);
        }

        @Specialization
        public boolean not(boolean value) {
            return !value;
        }

    }

    @CoreMethod(names = "!=", required = 1)
    public abstract static class NotEqualNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode equalNode;

        public NotEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public boolean equal(VirtualFrame frame, Object a, Object b) {
            return !equalNode.callBoolean(frame, a, "==", null, b);
        }

    }

    @CoreMethod(names = { "equal?", "==" }, required = 1)
    public abstract static class ReferenceEqualNode extends BinaryCoreMethodNode {

        public ReferenceEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract boolean executeReferenceEqual(VirtualFrame frame, Object a, Object b);

        @Specialization public boolean equal(boolean a, boolean b) { return a == b; }
        @Specialization public boolean equal(int a, int b) { return a == b; }
        @Specialization public boolean equal(long a, long b) { return a == b; }
        @Specialization public boolean equal(double a, double b) { return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b); }

        @Specialization public boolean equal(DynamicObject a, DynamicObject b) {
            return a == b;
        }

        @Specialization(guards = {"isNotDynamicObject(a)", "isNotDynamicObject(b)", "notSameClass(a, b)"})
        public boolean equal(Object a, Object b) {
            return false;
        }

        @Specialization(guards = "isNotDynamicObject(a)")
        public boolean equal(Object a, DynamicObject b) {
            return false;
        }

        @Specialization(guards = "isNotDynamicObject(b)")
        public boolean equal(DynamicObject a, Object b) {
            return false;
        }

        protected boolean isNotDynamicObject(Object value) {
            return !(value instanceof DynamicObject);
        }

        protected boolean notSameClass(Object a, Object b) {
            return a.getClass() != b.getClass();
        }

    }

    @CoreMethod(names = "initialize", needsSelf = false)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject initialize() {
            return nil();
        }

    }

    @CoreMethod(names = "instance_eval", needsBlock = true, optional = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class InstanceEvalNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yield;

        public InstanceEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context, DeclarationContext.INSTANCE_EVAL);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public Object instanceEval(Object receiver, DynamicObject string, NotProvided block) {
            return getContext().instanceEval(StringOperations.getByteListReadOnly(string), receiver, "(eval)", this);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object receiver, NotProvided string, DynamicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, receiver, receiver);
        }

    }

    @CoreMethod(names = "instance_exec", needsBlock = true, rest = true)
    public abstract static class InstanceExecNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yield;

        public InstanceExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context, DeclarationContext.INSTANCE_EVAL);
        }

        @Specialization
        public Object instanceExec(VirtualFrame frame, Object receiver, Object[] arguments, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();

            return yield.dispatchWithModifiedSelf(frame, block, receiver, arguments);
        }

        @Specialization
        public Object instanceExec(Object receiver, Object[] arguments, NotProvided block) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().localJumpError("no block given", this));
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "__instance_variables__")
    public abstract static class InstanceVariablesNode extends CoreMethodArrayArgumentsNode {

        public InstanceVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract DynamicObject executeObject(DynamicObject self);

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

    @CoreMethod(names = "method_missing", needsBlock = true, rest = true, optional = 1, visibility = Visibility.PRIVATE)
    public abstract static class MethodMissingNode extends CoreMethodArrayArgumentsNode {

        public MethodMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object methodMissingNoName(Object self, NotProvided name, Object[] args, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("no id given", this));
        }

        @Specialization
        public Object methodMissingNoBlock(Object self, DynamicObject name, Object[] args, NotProvided block) {
            return methodMissing(self, name, args, null);
        }

        @Specialization
        public Object methodMissingBlock(Object self, DynamicObject name, Object[] args, DynamicObject block) {
            return methodMissing(self, name, args, block);
        }

        @TruffleBoundary
        private Object methodMissing(Object self, DynamicObject nameObject, Object[] args, DynamicObject block) {
            final String name = nameObject.toString();

            if (lastCallWasSuper()) {
                throw new RaiseException(getContext().getCoreLibrary().noSuperMethodError(name, this));
            } else if (lastCallWasCallingPrivateMethod(self, name)) {
                throw new RaiseException(getContext().getCoreLibrary().privateMethodError(name, self, this));
            } else if (lastCallWasVCall()) {
                throw new RaiseException(getContext().getCoreLibrary().nameErrorUndefinedLocalVariableOrMethod(name, self, this));
            } else {
                throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnReceiver(name, self, this));
            }
        }

        private boolean lastCallWasSuper() {
            final SuperCallNode superCallNode = NodeUtil.findParent(Truffle.getRuntime().getCallerFrame().getCallNode(), SuperCallNode.class);
            return superCallNode != null;
        }

        /**
         * See {@link org.jruby.truffle.nodes.dispatch.DispatchNode#lookup}.
         * The only way to fail if method is not null and not undefined is visibility.
         */
        private boolean lastCallWasCallingPrivateMethod(Object self, String name) {
            final InternalMethod method = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(self), name);
            return method != null && !method.isUndefined();
        }

        private boolean lastCallWasVCall() {
            final RubyCallNode callNode = NodeUtil.findParent(Truffle.getRuntime().getCallerFrame().getCallNode(), RubyCallNode.class);
            return callNode != null && callNode.isVCall();
        }

    }

    @CoreMethod(names = "__send__", needsBlock = true, rest = true, required = 1)
    public abstract static class SendNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode dispatchNode;

        public SendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatchNode = new CallDispatchHeadNode(context, true,
                    MissingBehavior.CALL_METHOD_MISSING);
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, NotProvided block) {
            return send(frame, self, name, args, (DynamicObject) null);
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, DynamicObject block) {
            return dispatchNode.call(frame, self, name, block, args);
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass);
        }

    }

}
