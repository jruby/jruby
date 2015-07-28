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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.*;

@CoreClass(name = "BasicObject")
public abstract class BasicObjectNodes {

    public static final HiddenKey OBJECT_ID_IDENTIFIER = new HiddenKey("object_id");
    public static final HiddenKey TAINTED_IDENTIFIER = new HiddenKey("tainted?");
    public static final HiddenKey FROZEN_IDENTIFIER = new HiddenKey("frozen?");
    public static final Layout LAYOUT = Layout.createLayout(Layout.INT_TO_LONG);
    public static final Shape EMPTY_SHAPE = LAYOUT.createShape(new BasicObjectType());

    @CompilerDirectives.TruffleBoundary
    public static void setInstanceVariable(RubyBasicObject receiver, Object name, Object value) {
        Shape shape = getDynamicObject(receiver).getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            property.setGeneric(getDynamicObject(receiver), value, null);
        } else {
            getDynamicObject(receiver).define(name, value, 0);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static void setInstanceVariables(RubyBasicObject receiver, Map<Object, Object> instanceVariables) {
        for (Map.Entry<Object, Object> entry : instanceVariables.entrySet()) {
            setInstanceVariable(receiver, entry.getKey(), entry.getValue());
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static Object getInstanceVariable2(RubyBasicObject receiver, Object name) {
        Shape shape = getDynamicObject(receiver).getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            return property.get(getDynamicObject(receiver), false);
        } else {
            return getContext(receiver).getCoreLibrary().getNilObject();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static Map<Object, Object> getInstanceVariables(RubyBasicObject receiver) {
        Shape shape = getDynamicObject(receiver).getShape();
        Map<Object, Object> vars = new LinkedHashMap<>();
        List<Property> properties = shape.getPropertyList();
        for (Property property : properties) {
            vars.put((String) property.getKey(), property.get(getDynamicObject(receiver), false));
        }
        return vars;
    }

    @CompilerDirectives.TruffleBoundary
    public static Object[] getFieldNames(RubyBasicObject receiver) {
        List<Object> keys = getDynamicObject(receiver).getShape().getKeyList();
        return keys.toArray(new Object[keys.size()]);
    }

    @CompilerDirectives.TruffleBoundary
    public static boolean isFieldDefined(RubyBasicObject receiver, String name) {
        return getDynamicObject(receiver).getShape().hasProperty(name);
    }

    public static void unsafeSetLogicalClass(RubyBasicObject object, RubyBasicObject newLogicalClass) {
        assert RubyGuards.isRubyClass(newLogicalClass);
        assert object.logicalClass == null;
        unsafeChangeLogicalClass(object, newLogicalClass);
    }

    public static void unsafeChangeLogicalClass(RubyBasicObject object, RubyBasicObject newLogicalClass) {
        assert RubyGuards.isRubyClass(newLogicalClass);
        object.logicalClass = newLogicalClass;
        object.metaClass = newLogicalClass;
    }

    public static RubyBasicObject getMetaClass(RubyBasicObject object) {
        return object.metaClass;
    }

    public static void setMetaClass(RubyBasicObject object, RubyBasicObject metaClass) {
        assert RubyGuards.isRubyClass(metaClass);
        object.metaClass = metaClass;
    }

    @CompilerDirectives.TruffleBoundary
    public static long verySlowGetObjectID(RubyBasicObject object) {
        // TODO(CS): we should specialise on reading this in the #object_id method and anywhere else it's used
        Property property = object.dynamicObject.getShape().getProperty(OBJECT_ID_IDENTIFIER);

        if (property != null) {
            return (long) property.get(object.dynamicObject, false);
        }

        final long objectID = getContext(object).getNextObjectID();
        object.dynamicObject.define(OBJECT_ID_IDENTIFIER, objectID, 0);
        return objectID;
    }

    public static Object getInstanceVariable(RubyBasicObject object, String name) {
        final Object value = getInstanceVariable2(object, name);

        if (value == null) {
            return getContext(object).getCoreLibrary().getNilObject();
        } else {
            return value;
        }
    }

    public static void visitObjectGraph(RubyBasicObject object, ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (visitor.visit(object)) {
            visitObjectGraph(getMetaClass(object), visitor);

            for (Object instanceVariable : getInstanceVariables(object).values()) {
                if (instanceVariable instanceof RubyBasicObject) {
                    visitObjectGraph(((RubyBasicObject) instanceVariable), visitor);
                }
            }

            visitObjectGraphChildren(object, visitor);
        }
    }

    public static void visitObjectGraphChildren(RubyBasicObject rubyBasicObject, ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (RubyGuards.isRubyArray(rubyBasicObject)) {
            for (Object object : ArrayNodes.slowToArray(rubyBasicObject)) {
                if (object instanceof RubyBasicObject) {
                    visitObjectGraph(((RubyBasicObject) object), visitor);
                }
            }
        } else if (RubyGuards.isRubyHash(rubyBasicObject)) {
            for (Map.Entry<Object, Object> keyValue : HashNodes.iterableKeyValues(rubyBasicObject)) {
                if (keyValue.getKey() instanceof RubyBasicObject) {
                    visitObjectGraph(((RubyBasicObject) keyValue.getKey()), visitor);
                }

                if (keyValue.getValue() instanceof RubyBasicObject) {
                    visitObjectGraph(((RubyBasicObject) keyValue.getValue()), visitor);
                }
            }
        } else if (RubyGuards.isRubyBinding(rubyBasicObject)) {
            getContext(rubyBasicObject).getObjectSpaceManager().visitFrame(BindingNodes.getFrame(rubyBasicObject), visitor);
        } else if (RubyGuards.isRubyProc(rubyBasicObject)) {
            getContext(rubyBasicObject).getObjectSpaceManager().visitFrame(ProcNodes.getDeclarationFrame(rubyBasicObject), visitor);
        } else if (RubyGuards.isRubyMatchData(rubyBasicObject)) {
            for (Object object : MatchDataNodes.getFields(rubyBasicObject).values) {
                if (object instanceof RubyBasicObject) {
                    visitObjectGraph(((RubyBasicObject) object), visitor);
                }
            }
        } else if (RubyGuards.isObjectRange(rubyBasicObject)) {
            if (RangeNodes.OBJECT_RANGE_LAYOUT.getBegin(getDynamicObject(rubyBasicObject)) instanceof RubyBasicObject) {
                visitObjectGraph(((RubyBasicObject) RangeNodes.OBJECT_RANGE_LAYOUT.getBegin(getDynamicObject(rubyBasicObject))), visitor);
            }

            if (RangeNodes.OBJECT_RANGE_LAYOUT.getEnd(getDynamicObject(rubyBasicObject)) instanceof RubyBasicObject) {
                visitObjectGraph(((RubyBasicObject) RangeNodes.OBJECT_RANGE_LAYOUT.getEnd(getDynamicObject(rubyBasicObject))), visitor);
            }
        } else if (RubyGuards.isRubyModule(rubyBasicObject)) {
            ModuleNodes.getModel(rubyBasicObject).visitObjectGraphChildren(visitor);
        }
    }

    public static boolean isNumeric(RubyBasicObject rubyBasicObject) {
        return ModuleOperations.assignableTo(getMetaClass(rubyBasicObject), getContext(rubyBasicObject).getCoreLibrary().getNumericClass());
    }

    public static RubyContext getContext(RubyBasicObject rubyBasicObject) {
        if (RubyGuards.isRubyModule(rubyBasicObject)) {
            return ModuleNodes.getModel(rubyBasicObject).getContext();
        } else {
            return getContext(rubyBasicObject.logicalClass);
        }
    }

    public static RubyBasicObject getLogicalClass(RubyBasicObject rubyBasicObject) {
        return rubyBasicObject.logicalClass;
    }

    public static DynamicObject getDynamicObject(RubyBasicObject rubyBasicObject) {
        return rubyBasicObject.dynamicObject;
    }

    public static ForeignAccessFactory getForeignAccessFactory(RubyBasicObject object) {
        if (RubyGuards.isRubyArray(object)) {
            return new ArrayForeignAccessFactory(getContext(object));
        } else if (RubyGuards.isRubyHash(object)) {
            return new HashForeignAccessFactory(getContext(object));
        } else if (RubyGuards.isRubyString(object)) {
            return new StringForeignAccessFactory(getContext(object));
        } else {
            return new BasicForeignAccessFactory(getContext(object));
        }
    }

    public static String toString(RubyBasicObject object) {
        CompilerAsserts.neverPartOfCompilation("RubyBasicObject#toString should only be used for debugging");

        if (RubyGuards.isRubyString(object)) {
            return Helpers.decodeByteList(getContext(object).getRuntime(), StringNodes.getByteList(object));
        } else if (RubyGuards.isRubySymbol(object)) {
            return SymbolNodes.getString(object);
        } else if (RubyGuards.isRubyException(object)) {
            return ExceptionNodes.getMessage(object) + " : " + Objects.toString(object) + "\n" +
                    Arrays.toString(Backtrace.EXCEPTION_FORMATTER.format(getContext(object), object, ExceptionNodes.getBacktrace(object)));
        } else if (RubyGuards.isRubyModule(object)) {
            return ModuleNodes.getModel(object).toString();
        } else {
            return String.format("RubyBasicObject@%x<logicalClass=%s>", System.identityHashCode(object), ModuleNodes.getModel(getLogicalClass(object)).getName());
        }
    }

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
        public RubyBasicObject initialize() {
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

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public Object instanceEval(Object receiver, RubyBasicObject string, NotProvided block) {
            return getContext().instanceEval(StringNodes.getByteList(string), receiver, this);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object instanceEval(VirtualFrame frame, Object receiver, NotProvided string, RubyBasicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, receiver, receiver);
        }

    }

    @CoreMethod(names = "instance_exec", needsBlock = true, argumentsAsArray = true)
    public abstract static class InstanceExecNode extends YieldingCoreMethodNode {

        public InstanceExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object instanceExec(VirtualFrame frame, Object receiver, Object[] arguments, RubyBasicObject block) {
            CompilerDirectives.transferToInterpreter();

            return yieldWithModifiedSelf(frame, block, receiver, arguments);
        }

        @Specialization
        public Object instanceExec(Object receiver, Object[] arguments, NotProvided block) {
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
        public Object methodMissing(Object self, Object[] args, NotProvided block) {
            CompilerDirectives.transferToInterpreter();

            return methodMissing(self, args, (RubyBasicObject) null);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object methodMissing(Object self, Object[] args, RubyBasicObject block) {
            CompilerDirectives.transferToInterpreter();

            final RubyBasicObject name = (RubyBasicObject) args[0];
            final Object[] sentArgs = ArrayUtils.extractRange(args, 1, args.length);
            return methodMissing(self, name, sentArgs, block);
        }

        private Object methodMissing(Object self, RubyBasicObject name, Object[] args, RubyBasicObject block) {
            CompilerDirectives.transferToInterpreter();

            assert block == null || RubyGuards.isRubyProc(block);

            // TODO: should not be a call to Java toString(), but rather sth like name_err_mesg_to_str() in MRI error.c
            if (lastCallWasVCall()) {
                throw new RaiseException(
                        getContext().getCoreLibrary().nameErrorUndefinedLocalVariableOrMethod(
                                SymbolNodes.getString(name),
                                ModuleNodes.getModel(getContext().getCoreLibrary().getLogicalClass(self)).getName(),
                                this));
            } else {
                throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnReceiver(SymbolNodes.getString(name), self, this));
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

            dispatchNode = new CallDispatchHeadNode(context, true,
                    DispatchNode.DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT,
                    MissingBehavior.CALL_METHOD_MISSING);

            if (DispatchNode.DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                dispatchNode.forceUncached();
            }
        }

        @Specialization
        public Object send(VirtualFrame frame, Object self, Object[] args, NotProvided block) {
            return send(frame, self, args, (RubyBasicObject) null);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object send(VirtualFrame frame, Object self, Object[] args, RubyBasicObject block) {
            final Object name = args[0];
            final Object[] sendArgs = ArrayUtils.extractRange(args, 1, args.length);
            return dispatchNode.call(frame, self, name, block, sendArgs);
        }

    }

    public static class BasicObjectAllocator implements Allocator {

        // TODO(CS): why on earth is this a boundary? Seems like a really bad thing.
        @CompilerDirectives.TruffleBoundary
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            return new RubyBasicObject(rubyClass, LAYOUT.newInstance(EMPTY_SHAPE));
        }

    }
}
