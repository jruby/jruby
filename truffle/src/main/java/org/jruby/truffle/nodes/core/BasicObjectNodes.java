/*
 * Copyb (c) 2013, 2015 Oracle and/or its affiliates. All bs reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyCallNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.util.ArrayUtils;

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


    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends BinaryCoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization public boolean equal(boolean a, boolean b) { return a == b; }
        @Specialization public boolean equal(int a, int b) { return a == b; }
        @Specialization public boolean equal(long a, long b) { return a == b; }
        @Specialization public boolean equal(double a, double b) { return a == b; }

        @Specialization public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a == b;
        }

        @Specialization(guards = {"isNotRubyBasicObject(a)", "isNotRubyBasicObject(b)", "notSameClass(a, b)"})
        public boolean equal(Object a, Object b) {
            return false;
        }

        @Specialization(guards = "isNotRubyBasicObject(a)")
        public boolean equal(Object a, RubyBasicObject b) {
            return false;
        }

        @Specialization(guards = "isNotRubyBasicObject(b)")
        public boolean equal(RubyBasicObject a, Object b) {
            return false;
        }

        protected boolean isNotRubyBasicObject(Object value) {
            return !(value instanceof RubyBasicObject);
        }

        protected boolean notSameClass(Object a, Object b) {
            return a.getClass() != b.getClass();
        }

    }

    @CoreMethod(names = "!=", required = 1)
    public abstract static class NotEqualNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode equalNode;

        public NotEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        @Specialization
        public boolean equal(VirtualFrame frame, Object a, Object b) {
            return !equalNode.callBoolean(frame, a, "==", null, b);
        }

    }

    @CoreMethod(names = "equal?", required = 1)
    public abstract static class ReferenceEqualNode extends BinaryCoreMethodNode {

        public ReferenceEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract boolean executeReferenceEqual(VirtualFrame frame, Object a, Object b);

        @Specialization public boolean equal(boolean a, boolean b) { return a == b; }
        @Specialization public boolean equal(int a, int b) { return a == b; }
        @Specialization public boolean equal(long a, long b) { return a == b; }
        @Specialization public boolean equal(double a, double b) { return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b); }

        @Specialization public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a == b;
        }

        @Specialization(guards = {"isNotRubyBasicObject(a)", "isNotRubyBasicObject(b)", "notSameClass(a, b)"})
        public boolean equal(Object a, Object b) {
            return false;
        }

        @Specialization(guards = "isNotRubyBasicObject(a)")
        public boolean equal(Object a, RubyBasicObject b) {
            return false;
        }

        @Specialization(guards = "isNotRubyBasicObject(b)")
        public boolean equal(RubyBasicObject a, Object b) {
            return false;
        }

        protected boolean isNotRubyBasicObject(Object value) {
            return !(value instanceof RubyBasicObject);
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
        public RubyNilClass initialize() {
            return nil();
        }

    }

    @CoreMethod(names = "instance_eval", needsBlock = true, optional = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class InstanceEvalNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yield;

        public InstanceEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object receiver, RubyString string, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            return getContext().instanceEval(string.getByteList(), receiver, this);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object receiver, UndefinedPlaceholder string, RubyProc block) {
            return yield.dispatchWithModifiedSelf(frame, block, receiver, receiver);
        }

    }

    @CoreMethod(names = "instance_exec", needsBlock = true, argumentsAsArray = true)
    public abstract static class InstanceExecNode extends YieldingCoreMethodNode {

        public InstanceExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object instanceExec(VirtualFrame frame, Object receiver, Object[] arguments, RubyProc block) {
            notDesignedForCompilation();

            return yieldWithModifiedSelf(frame, block, receiver, arguments);
        }

        @Specialization
        public Object instanceExec(Object receiver, Object[] arguments, UndefinedPlaceholder block) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().localJumpError("no block given", this));
        }

    }

    @CoreMethod(names = "method_missing", needsBlock = true, argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class MethodMissingNode extends CoreMethodArrayArgumentsNode {

        public MethodMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object methodMissing(Object self, Object[] args, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            return methodMissing(self, args, (RubyProc) null);
        }

        @Specialization
        public Object methodMissing(Object self, Object[] args, RubyProc block) {
            notDesignedForCompilation();

            final RubySymbol name = (RubySymbol) args[0];
            final Object[] sentArgs = ArrayUtils.extractRange(args, 1, args.length);
            return methodMissing(self, name, sentArgs, block);
        }

        private Object methodMissing(Object self, RubySymbol name, Object[] args, RubyProc block) {
            CompilerDirectives.transferToInterpreter();
            // TODO: should not be a call to Java toString(), but rather sth like name_err_mesg_to_str() in MRI error.c
            if (lastCallWasVCall()) {
                throw new RaiseException(
                        getContext().getCoreLibrary().nameErrorUndefinedLocalVariableOrMethod(
                                name.toString(),
                                getContext().getCoreLibrary().getLogicalClass(self).getName(),
                                this));
            } else {
                throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnReceiver(name.toString(), self, this));
            }
        }

        private boolean lastCallWasVCall() {
            final RubyCallNode callNode = NodeUtil.findParent(Truffle.getRuntime().getCallerFrame().getCallNode(), RubyCallNode.class);

            if (callNode == null) {
                return false;
            }

            return callNode.isVCall();

        }

    }

    @CoreMethod(names = "__send__", needsBlock = true, required = 1, argumentsAsArray = true)
    public abstract static class SendNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode dispatchNode;

        public SendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatchNode = DispatchHeadNodeFactory.createMethodCall(context, true, DispatchNode.DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT, MissingBehavior.CALL_METHOD_MISSING);

            if (DispatchNode.DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                dispatchNode.forceUncached();
            }
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object[] args, UndefinedPlaceholder block) {
            return send(frame, self, args, (RubyProc) null);
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object[] args, RubyProc block) {
            final Object name = args[0];
            final Object[] sendArgs = ArrayUtils.extractRange(args, 1, args.length);
            return dispatchNode.call(frame, self, name, block, sendArgs);
        }

    }

}
