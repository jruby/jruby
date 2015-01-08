/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyCallNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.dispatch.PredicateDispatchHeadNode;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.ObjectIDOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.cli.Options;

import java.util.Arrays;

@CoreClass(name = "BasicObject")
public abstract class BasicObjectNodes {

    @CoreMethod(names = "!")
    public abstract static class NotNode extends UnaryCoreMethodNode {

        public NotNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NotNode(NotNode prev) {
            super(prev);
        }

        @CreateCast("operand") public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeFactory.create(getContext(), getSourceSection(), operand);
        }

        @Specialization
        public boolean not(boolean value) {
            return !value;
        }

    }

    @CoreMethod(names = "!=", required = 1)
    public abstract static class NotEqualNode extends CoreMethodNode {

        @Child private PredicateDispatchHeadNode equalNode;

        public NotEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = new PredicateDispatchHeadNode(context);
        }

        public NotEqualNode(NotEqualNode prev) {
            super(prev);
            equalNode = prev.equalNode;
        }

        @Specialization
        public boolean equal(VirtualFrame frame, Object a, Object b) {
            return !equalNode.call(frame, a, "==", null, b);
        }

    }

    @CoreMethod(names = "__id__")
    public abstract static class IDNode extends CoreMethodNode {

        public IDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IDNode(IDNode prev) {
            super(prev);
        }

        public abstract Object executeObjectID(VirtualFrame frame, Object value);

        @Specialization
        public int objectID(RubyNilClass nil) {
            return ObjectIDOperations.NIL;
        }

        @Specialization(guards = "isTrue")
        public int objectIDTrue(boolean value) {
            return ObjectIDOperations.TRUE;
        }

        @Specialization(guards = "!isTrue")
        public int objectIDFalse(boolean value) {
            return ObjectIDOperations.FALSE;
        }

        @Specialization
        public long objectID(int value) {
            return ObjectIDOperations.smallFixnumToID(value);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long objectIDSmallFixnumOverflow(long value) {
            return ObjectIDOperations.smallFixnumToIDOverflow(value);
        }

        /* TODO: Ideally we would have this instead of the code below to speculate better. [GRAAL-903]
        @Specialization(guards = "isSmallFixnum")
        public long objectIDSmallFixnum(long value) {
            return ObjectIDOperations.smallFixnumToID(value);
        }

        @Specialization(guards = "!isSmallFixnum")
        public Object objectIDLargeFixnum(long value) {
            return ObjectIDOperations.largeFixnumToID(getContext(), value);
        } */

        @Specialization
        public Object objectID(long value) {
            if (isSmallFixnum(value)) {
                return ObjectIDOperations.smallFixnumToID(value);
            } else {
                return ObjectIDOperations.largeFixnumToID(getContext(), value);
            }
        }

        @Specialization
        public RubyBignum objectID(double value) {
            return ObjectIDOperations.floatToID(getContext(), value);
        }

        @Specialization
        public long objectID(RubyBasicObject object) {
            return object.getObjectID();
        }

        protected boolean isSmallFixnum(long fixnum) {
            return ObjectIDOperations.isSmallFixnum(fixnum);
        }

    }

    @CoreMethod(names = {"equal?", "=="}, required = 1)
    public abstract static class ReferenceEqualNode extends BinaryCoreMethodNode {

        public ReferenceEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReferenceEqualNode(ReferenceEqualNode prev) {
            super(prev);
        }

        protected abstract boolean executeReferenceEqual(VirtualFrame frame, Object a, Object b);

        @Specialization public boolean equal(boolean a, boolean b) { return a == b; }
        @Specialization public boolean equal(int a, int b) { return a == b; }
        @Specialization public boolean equal(long a, long b) { return a == b; }
        @Specialization public boolean equal(double a, double b) { return a == b; }

        @Specialization public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a == b;
        }

        @Specialization(guards = {"isNotRubyBasicObject(left)", "isNotRubyBasicObject(right)", "notSameClass"})
        public boolean equal(Object a, Object b) {
            return false;
        }

        @Specialization(guards = "isNotRubyBasicObject(left)")
        public boolean equal(Object a, RubyBasicObject b) {
            return false;
        }

        @Specialization(guards = "isNotRubyBasicObject(right)")
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

    @CoreMethod(names = "initialize", needsSelf = false, visibility = Visibility.PRIVATE)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass initialize() {
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "instance_eval", needsBlock = true, optional = 1)
    public abstract static class InstanceEvalNode extends CoreMethodNode {

        @Child private YieldDispatchHeadNode yield;

        public InstanceEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldDispatchHeadNode(context);
        }

        public InstanceEvalNode(InstanceEvalNode prev) {
            super(prev);
            yield = prev.yield;
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object receiver, RubyString string, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            return getContext().eval(string.getBytes(), receiver, this);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object receiver, UndefinedPlaceholder string, RubyProc block) {
            notDesignedForCompilation();

            return yield.dispatchWithModifiedSelf(frame, block, receiver);
        }

    }

    @CoreMethod(names = "method_missing", needsBlock = true, argumentsAsArray = true, visibility = Visibility.PRIVATE)
    public abstract static class MethodMissingNode extends CoreMethodNode {

        public MethodMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodMissingNode(MethodMissingNode prev) {
            super(prev);
        }

        @Specialization
        public Object methodMissing(Object self, Object[] args, @SuppressWarnings("unused") UndefinedPlaceholder block) {
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
            // TODO: should not be a call to Java toString(), but rather sth like name_err_mesg_to_str() in MRI error.c
            if (lastCallWasVCall()) {
                throw new RaiseException(
                        getContext().getCoreLibrary().nameErrorUndefinedLocalVariableOrMethod(
                                name.toString(),
                                getContext().getCoreLibrary().getLogicalClass(self).getName(),
                                this));
            } else {
                throw new RaiseException(
                        getContext().getCoreLibrary().noMethodError(
                                name.toString(),
                                getContext().getCoreLibrary().getLogicalClass(self).getName(),
                                this));
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
    public abstract static class SendNode extends CoreMethodNode {

        @Child private DispatchHeadNode dispatchNode;

        public SendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatchNode = new DispatchHeadNode(context, true, Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT.load(), MissingBehavior.CALL_METHOD_MISSING);

            if (Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED.load()) {
                dispatchNode.forceUncached();
            }
        }

        public SendNode(SendNode prev) {
            super(prev);
            dispatchNode = prev.dispatchNode;
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
