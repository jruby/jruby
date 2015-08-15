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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.*;
import com.oracle.truffle.api.object.DynamicObject;

@CoreClass(name = "BasicObject")
public abstract class BasicObjectNodes {

    public static final HiddenKey OBJECT_ID_IDENTIFIER = new HiddenKey("object_id");
    public static final HiddenKey TAINTED_IDENTIFIER = new HiddenKey("tainted?");
    public static final HiddenKey FROZEN_IDENTIFIER = new HiddenKey("frozen?");

    @org.jruby.truffle.om.dsl.api.Layout(objectTypeSuperclass = "org.jruby.truffle.runtime.RubyObjectType")
    public interface BasicObjectLayout {

        DynamicObjectFactory createBasicObjectShape(@Nullable DynamicObject logicalClass, @Nullable DynamicObject metaClass);

        DynamicObject createBasicObject(DynamicObjectFactory factory);

        boolean isBasicObject(Object object);

        DynamicObjectFactory setLogicalClass(DynamicObjectFactory factory, DynamicObject value);
        DynamicObject getLogicalClass(ObjectType objectType);
        DynamicObject getLogicalClass(DynamicObject object);
        void setLogicalClass(DynamicObject object, DynamicObject value);

        DynamicObjectFactory setMetaClass(DynamicObjectFactory factory, DynamicObject value);
        DynamicObject getMetaClass(DynamicObject object);
        void setMetaClass(DynamicObject object, DynamicObject value);
    }

    public static final BasicObjectLayout BASIC_OBJECT_LAYOUT = BasicObjectLayoutImpl.INSTANCE;

    @CompilerDirectives.TruffleBoundary
    public static void setInstanceVariable(DynamicObject receiver, Object name, Object value) {
        Shape shape = receiver.getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            property.setGeneric(receiver, value, null);
        } else {
            receiver.define(name, value, 0);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static void setInstanceVariables(DynamicObject receiver, Map<Object, Object> instanceVariables) {
        for (Map.Entry<Object, Object> entry : instanceVariables.entrySet()) {
            setInstanceVariable(receiver, entry.getKey(), entry.getValue());
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static Object getInstanceVariable2(DynamicObject receiver, Object name) {
        Shape shape = receiver.getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            return property.get(receiver, false);
        } else {
            return getContext(receiver).getCoreLibrary().getNilObject();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static Map<Object, Object> getInstanceVariables(DynamicObject receiver) {
        Shape shape = receiver.getShape();
        Map<Object, Object> vars = new LinkedHashMap<>();
        List<Property> properties = shape.getPropertyList();
        for (Property property : properties) {
            vars.put((String) property.getKey(), property.get(receiver, false));
        }
        return vars;
    }

    @CompilerDirectives.TruffleBoundary
    public static Object[] getFieldNames(DynamicObject receiver) {
        List<Object> keys = receiver.getShape().getKeyList();
        return keys.toArray(new Object[keys.size()]);
    }

    @CompilerDirectives.TruffleBoundary
    public static boolean isFieldDefined(DynamicObject receiver, String name) {
        return receiver.getShape().hasProperty(name);
    }

    public static DynamicObject getMetaClass(DynamicObject object) {
        return BASIC_OBJECT_LAYOUT.getMetaClass(object);
    }

    public static void setMetaClass(DynamicObject object, DynamicObject metaClass) {
        BASIC_OBJECT_LAYOUT.setMetaClass(object, metaClass);
    }

    @CompilerDirectives.TruffleBoundary
    public static long verySlowGetObjectID(DynamicObject object) {
        // TODO(CS): we should specialise on reading this in the #object_id method and anywhere else it's used
        Property property = object.getShape().getProperty(OBJECT_ID_IDENTIFIER);

        if (property != null) {
            return (long) property.get(object, false);
        }

        final long objectID = getContext(object).getNextObjectID();
        object.define(OBJECT_ID_IDENTIFIER, objectID, 0);
        return objectID;
    }

    public static Object getInstanceVariable(DynamicObject object, String name) {
        final Object value = getInstanceVariable2(object, name);

        if (value == null) {
            return getContext(object).getCoreLibrary().getNilObject();
        } else {
            return value;
        }
    }

    public static void visitObjectGraph(DynamicObject object, ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (visitor.visit(object)) {
            visitObjectGraph(getMetaClass(object), visitor);

            for (Object instanceVariable : getInstanceVariables(object).values()) {
                if (instanceVariable instanceof DynamicObject) {
                    visitObjectGraph(((DynamicObject) instanceVariable), visitor);
                }
            }

            visitObjectGraphChildren(object, visitor);
        }
    }

    public static void visitObjectGraphChildren(DynamicObject rubyBasicObject, ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (RubyGuards.isRubyArray(rubyBasicObject)) {
            for (Object object : ArrayNodes.slowToArray(rubyBasicObject)) {
                if (object instanceof DynamicObject) {
                    visitObjectGraph(((DynamicObject) object), visitor);
                }
            }
        } else if (RubyGuards.isRubyHash(rubyBasicObject)) {
            for (Map.Entry<Object, Object> keyValue : HashNodes.iterableKeyValues(rubyBasicObject)) {
                if (keyValue.getKey() instanceof DynamicObject) {
                    visitObjectGraph(((DynamicObject) keyValue.getKey()), visitor);
                }

                if (keyValue.getValue() instanceof DynamicObject) {
                    visitObjectGraph(((DynamicObject) keyValue.getValue()), visitor);
                }
            }
        } else if (RubyGuards.isRubyBinding(rubyBasicObject)) {
            getContext(rubyBasicObject).getObjectSpaceManager().visitFrame(BindingNodes.getFrame(rubyBasicObject), visitor);
        } else if (RubyGuards.isRubyProc(rubyBasicObject)) {
            getContext(rubyBasicObject).getObjectSpaceManager().visitFrame(ProcNodes.getDeclarationFrame(rubyBasicObject), visitor);
        } else if (RubyGuards.isRubyMatchData(rubyBasicObject)) {
            for (Object object : MatchDataNodes.getFields(rubyBasicObject).values) {
                if (object instanceof DynamicObject) {
                    visitObjectGraph(((DynamicObject) object), visitor);
                }
            }
        } else if (RubyGuards.isObjectRange(rubyBasicObject)) {
            if (RangeNodes.OBJECT_RANGE_LAYOUT.getBegin(rubyBasicObject) instanceof DynamicObject) {
                visitObjectGraph(((DynamicObject) RangeNodes.OBJECT_RANGE_LAYOUT.getBegin(rubyBasicObject)), visitor);
            }

            if (RangeNodes.OBJECT_RANGE_LAYOUT.getEnd(rubyBasicObject) instanceof DynamicObject) {
                visitObjectGraph(((DynamicObject) RangeNodes.OBJECT_RANGE_LAYOUT.getEnd(rubyBasicObject)), visitor);
            }
        } else if (RubyGuards.isRubyModule(rubyBasicObject)) {
            ModuleNodes.getFields(rubyBasicObject).visitObjectGraphChildren(visitor);
        }
    }

    public static RubyContext getContext(DynamicObject rubyBasicObject) {
        if (RubyGuards.isRubyModule(rubyBasicObject)) {
            return ModuleNodes.getFields(rubyBasicObject).getContext();
        } else {
            return getContext(getLogicalClass(rubyBasicObject));
        }
    }

    public static DynamicObject getLogicalClass(DynamicObject rubyBasicObject) {
        return BASIC_OBJECT_LAYOUT.getLogicalClass(rubyBasicObject);
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
            yield = new YieldDispatchHeadNode(context);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public Object instanceEval(Object receiver, DynamicObject string, NotProvided block) {
            return getContext().instanceEval(StringNodes.getByteList(string), receiver, this);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object instanceEval(VirtualFrame frame, Object receiver, NotProvided string, DynamicObject block) {
            return yield.dispatchWithModifiedSelf(frame, block, receiver, receiver);
        }

    }

    @CoreMethod(names = "instance_exec", needsBlock = true, rest = true)
    public abstract static class InstanceExecNode extends YieldingCoreMethodNode {

        public InstanceExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object instanceExec(VirtualFrame frame, Object receiver, Object[] arguments, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();

            return yieldWithModifiedSelf(frame, block, receiver, arguments);
        }

        @Specialization
        public Object instanceExec(Object receiver, Object[] arguments, NotProvided block) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(getContext().getCoreLibrary().localJumpError("no block given", this));
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
            return methodMissingBlock(self, name, args, (DynamicObject) null);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object methodMissingBlock(Object self, DynamicObject name, Object[] args, DynamicObject block) {
            return methodMissing(self, name, args, block);
        }

        private Object methodMissing(Object self, DynamicObject name, Object[] args, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();

            assert block == null || RubyGuards.isRubyProc(block);

            // TODO: should not be a call to Java toString(), but rather sth like name_err_mesg_to_str() in MRI error.c
            if (lastCallWasVCall()) {
                throw new RaiseException(
                        getContext().getCoreLibrary().nameErrorUndefinedLocalVariableOrMethod(
                                SymbolNodes.getString(name),
                                ModuleNodes.getFields(getContext().getCoreLibrary().getLogicalClass(self)).getName(),
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

    @CoreMethod(names = "__send__", needsBlock = true, rest = true, required = 1)
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
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, NotProvided block) {
            return send(frame, self, name, args, (DynamicObject) null);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object send(VirtualFrame frame, Object self, Object name, Object[] args, DynamicObject block) {
            return dispatchNode.call(frame, self, name, block, args);
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.executeAllocate(rubyClass);
        }

    }

}
