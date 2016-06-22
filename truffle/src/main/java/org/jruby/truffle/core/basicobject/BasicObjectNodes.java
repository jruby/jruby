/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.basicobject;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.array.ArrayHelpers;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.dispatch.RubyCallNode;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.language.supercall.SuperCallNode;
import org.jruby.truffle.language.yield.YieldNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CoreClass("BasicObject")
public abstract class BasicObjectNodes {

    @CoreMethod(names = "!")
    public abstract static class NotNode extends UnaryCoreMethodNode {

        @CreateCast("operand")
        public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeGen.create(operand);
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
    public abstract static class ReferenceEqualNode extends CoreMethodArrayArgumentsNode {

        public abstract boolean executeReferenceEqual(VirtualFrame frame, Object a, Object b);

        @Specialization
        public boolean equal(boolean a, boolean b) {
            return a == b;
        }

        @Specialization
        public boolean equal(int a, int b) {
            return a == b;
        }

        @Specialization
        public boolean equal(long a, long b) {
            return a == b;
        }

        @Specialization
        public boolean equal(double a, double b) {
            return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b);
        }

        @Specialization
        public boolean equal(DynamicObject a, DynamicObject b) {
            return a == b;
        }

        @Specialization(guards = { "isNotDynamicObject(a)", "isNotDynamicObject(b)", "notSameClass(a, b)" })
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

        @Specialization
        public DynamicObject initialize() {
            return nil();
        }

    }

    @CoreMethod(names = "instance_eval", needsBlock = true, optional = 3, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class InstanceEvalNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldNode yield;

        public InstanceEvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldNode(context, DeclarationContext.INSTANCE_EVAL);
        }

        @Specialization(guards = { "isRubyString(string)", "isRubyString(fileName)" })
        public Object instanceEval(VirtualFrame frame, Object receiver, DynamicObject string, DynamicObject fileName, int line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            final Rope code = StringOperations.rope(string);

            // TODO (pitr 15-Oct-2015): fix this ugly hack, required for AS, copy-paste
            final String space = new String(new char[Math.max(line - 1, 0)]).replace("\0", "\n");
            final Source source = getContext().getSourceLoader().loadFragment(space + code.toString(), StringOperations.rope(fileName).toString());

            final RubyRootNode rootNode = getContext().getCodeLoader().parse(source, code.getEncoding(), ParserContext.EVAL, null, true, this);
            final CodeLoader.DeferredCall deferredCall = getContext().getCodeLoader().prepareExecute(ParserContext.EVAL, DeclarationContext.INSTANCE_EVAL, rootNode, null, receiver);
            return deferredCall.call(frame, callNode);
        }

        @Specialization(guards = { "isRubyString(string)", "isRubyString(fileName)" })
        public Object instanceEval(VirtualFrame frame, Object receiver, DynamicObject string, DynamicObject fileName, NotProvided line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            return instanceEval(frame, receiver, string, fileName, 1, block, callNode);
        }

        @Specialization(guards = { "isRubyString(string)" })
        public Object instanceEval(VirtualFrame frame, Object receiver, DynamicObject string, NotProvided fileName, NotProvided line, NotProvided block, @Cached("create()") IndirectCallNode callNode) {
            final DynamicObject eval = StringOperations.createString(getContext(), StringOperations.encodeRope("(eval)", ASCIIEncoding.INSTANCE));
            return instanceEval(frame, receiver, string, eval, 1, block, callNode);
        }

        @Specialization
        public Object instanceEval(VirtualFrame frame, Object receiver, NotProvided string, NotProvided fileName, NotProvided line, DynamicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, receiver, receiver);
        }

    }

    @CoreMethod(names = "instance_exec", needsBlock = true, rest = true)
    public abstract static class InstanceExecNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldNode yield;

        public InstanceExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yield = new YieldNode(context, DeclarationContext.INSTANCE_EVAL);
        }

        @Specialization
        public Object instanceExec(VirtualFrame frame, Object receiver, Object[] arguments, DynamicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, receiver, arguments);
        }

        @Specialization
        public Object instanceExec(Object receiver, Object[] arguments, NotProvided block) {
            throw new RaiseException(coreExceptions().localJumpError("no block given", this));
        }

    }

    @NonStandard
    @CoreMethod(names = "__instance_variables__")
    public abstract static class InstanceVariablesNode extends CoreMethodArrayArgumentsNode {

        public abstract DynamicObject executeObject(DynamicObject self);

        @TruffleBoundary
        @Specialization
        public DynamicObject instanceVariables(DynamicObject self) {
            List<Object> keys = self.getShape().getKeyList();

            final Object[] instanceVariableNames = keys.toArray(new Object[keys.size()]);
            Arrays.sort(instanceVariableNames);

            final List<Object> names = new ArrayList<>();
            for (Object name : instanceVariableNames) {
                if (name instanceof String) {
                    names.add(getSymbol((String) name));
                }
            }
            final int size = names.size();
            return ArrayHelpers.createArray(getContext(), names.toArray(new Object[size]), size);
        }

    }

    @CoreMethod(names = "method_missing", needsBlock = true, rest = true, optional = 1, visibility = Visibility.PRIVATE)
    public abstract static class MethodMissingNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object methodMissingNoName(Object self, NotProvided name, Object[] args, NotProvided block) {
            throw new RaiseException(coreExceptions().argumentError("no id given", this));
        }

        @Specialization
        public Object methodMissingNoBlock(Object self, DynamicObject name, Object[] args, NotProvided block) {
            return methodMissing(self, name, args, null);
        }

        @Specialization
        public Object methodMissingBlock(Object self, DynamicObject name, Object[] args, DynamicObject block) {
            return methodMissing(self, name, args, block);
        }

        private Object methodMissing(Object self, DynamicObject nameObject, Object[] args, DynamicObject block) {
            throw new RaiseException(buildMethodMissingException(self, nameObject, args, block));
        }

        @TruffleBoundary
        private DynamicObject buildMethodMissingException(Object self, DynamicObject nameObject, Object[] args, DynamicObject block) {
            final String name = nameObject.toString();

            if (lastCallWasSuper()) {
                return coreExceptions().noSuperMethodError(name, this);
            } else if (lastCallWasCallingPrivateMethod(self, name)) {
                return coreExceptions().privateMethodError(name, self, this);
            } else if (lastCallWasVCall()) {
                return coreExceptions().nameErrorUndefinedLocalVariableOrMethod(name, self, this);
            } else {
                return coreExceptions().noMethodErrorOnReceiver(name, self, this);
            }
        }

        private boolean lastCallWasSuper() {
            final SuperCallNode superCallNode = NodeUtil.findParent(Truffle.getRuntime().getCallerFrame().getCallNode(), SuperCallNode.class);
            return superCallNode != null;
        }

        /**
         * See {@link org.jruby.truffle.language.dispatch.DispatchNode#lookup}.
         * The only way to fail if method is not null and not undefined is visibility.
         */
        private boolean lastCallWasCallingPrivateMethod(Object self, String name) {
            final InternalMethod method = ModuleOperations.lookupMethod(coreLibrary().getMetaClass(self), name);
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
