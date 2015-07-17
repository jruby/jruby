/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the Ruby {@code BasicObject} class - the root of the Ruby class hierarchy.
 */
public class RubyBasicObject implements TruffleObject {

    public static final HiddenKey OBJECT_ID_IDENTIFIER = new HiddenKey("object_id");
    public static final HiddenKey TAINTED_IDENTIFIER = new HiddenKey("tainted?");
    public static final HiddenKey FROZEN_IDENTIFIER = new HiddenKey("frozen?");

    public static final Layout LAYOUT = Layout.createLayout(Layout.INT_TO_LONG);
    public static final Shape EMPTY_SHAPE = LAYOUT.createShape(new BasicObjectType());

    private final DynamicObject dynamicObject;

    /** The class of the object, not a singleton class. */
    @CompilationFinal private RubyClass logicalClass;
    /** Either the singleton class if it exists or the logicalClass. */
    @CompilationFinal private RubyClass metaClass;

    public RubyBasicObject(RubyClass rubyClass) {
        this(rubyClass.getContext(), rubyClass);
    }

    public RubyBasicObject(RubyClass rubyClass, DynamicObject dynamicObject) {
        this(rubyClass.getContext(), rubyClass, dynamicObject);
    }

    protected RubyBasicObject(RubyContext context, RubyClass rubyClass) {
        this(context, rubyClass, LAYOUT.newInstance(EMPTY_SHAPE));
    }

    private RubyBasicObject(RubyContext context, RubyClass rubyClass, DynamicObject dynamicObject) {
        this.dynamicObject = dynamicObject;

        if (rubyClass == null && this instanceof RubyClass) { // For class Class
            rubyClass = (RubyClass) this;
        }
        unsafeSetLogicalClass(rubyClass);
    }

    protected void unsafeSetLogicalClass(RubyClass newLogicalClass) {
        assert logicalClass == null;
        unsafeChangeLogicalClass(newLogicalClass);
    }

    public void unsafeChangeLogicalClass(RubyClass newLogicalClass) {
        logicalClass = newLogicalClass;
        metaClass = newLogicalClass;
    }

    public RubyClass getMetaClass() {
        return metaClass;
    }

    public void setMetaClass(RubyClass metaClass) {
        this.metaClass = metaClass;
    }

    @TruffleBoundary
    public long verySlowGetObjectID() {
        // TODO(CS): we should specialise on reading this in the #object_id method and anywhere else it's used
        Property property = dynamicObject.getShape().getProperty(OBJECT_ID_IDENTIFIER);

        if (property != null) {
            return (long) property.get(dynamicObject, false);
        }

        final long objectID = getContext().getNextObjectID();
        dynamicObject.define(OBJECT_ID_IDENTIFIER, objectID, 0);
        return objectID;
    }

    public Object getInstanceVariable(String name) {
        final Object value = getInstanceVariable(this, name);

        if (value == null) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return value;
        }
    }

    public boolean isFieldDefined(String name) {
        return isFieldDefined(this, name);
    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        if (RubyGuards.isRubyArray(this)) {
            return new ArrayForeignAccessFactory(getContext());
        } else if (RubyGuards.isRubyHash(this)) {
            return new HashForeignAccessFactory(getContext());
        } else if (RubyGuards.isRubyString(this)) {
            return new StringForeignAccessFactory(getContext());
        } else {
            return new BasicForeignAccessFactory(getContext());
        }
    }

    public final void visitObjectGraph(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (visitor.visit(this)) {
            getMetaClass().visitObjectGraph(visitor);

            for (Object instanceVariable : getInstanceVariables(this).values()) {
                if (instanceVariable instanceof RubyBasicObject) {
                    ((RubyBasicObject) instanceVariable).visitObjectGraph(visitor);
                }
            }

            visitObjectGraphChildren(visitor);
        }
    }

    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (RubyGuards.isRubyArray(this)) {
            for (Object object : ArrayNodes.slowToArray(this)) {
                if (object instanceof RubyBasicObject) {
                    ((RubyBasicObject) object).visitObjectGraph(visitor);
                }
            }
        } else if (RubyGuards.isRubyHash(this)) {
            for (Map.Entry<Object, Object> keyValue : HashNodes.iterableKeyValues(this)) {
                if (keyValue.getKey() instanceof RubyBasicObject) {
                    ((RubyBasicObject) keyValue.getKey()).visitObjectGraph(visitor);
                }

                if (keyValue.getValue() instanceof RubyBasicObject) {
                    ((RubyBasicObject) keyValue.getValue()).visitObjectGraph(visitor);
                }
            }
        } else if (RubyGuards.isRubyBinding(this)) {
            getContext().getObjectSpaceManager().visitFrame(BindingNodes.getFrame(this), visitor);
        } else if (RubyGuards.isRubyProc(this)) {
            getContext().getObjectSpaceManager().visitFrame(ProcNodes.getDeclarationFrame(this), visitor);
        } else if (RubyGuards.isRubyMatchData(this)) {
            for (Object object : ((RubyMatchData) this).fields.values) {
                if (object instanceof RubyBasicObject) {
                    ((RubyBasicObject) object).visitObjectGraph(visitor);
                }
            }
        } else if (RubyGuards.isObjectRange(this)) {
            if (((RubyObjectRange) this).begin instanceof RubyBasicObject) {
                ((RubyBasicObject) ((RubyObjectRange) this).begin).visitObjectGraph(visitor);
            }

            if (((RubyObjectRange) this).end instanceof RubyBasicObject) {
                ((RubyBasicObject) ((RubyObjectRange) this).end).visitObjectGraph(visitor);
            }
        }
    }

    public boolean isNumeric() {
        return ModuleOperations.assignableTo(this.getMetaClass(), getContext().getCoreLibrary().getNumericClass());
    }

    public RubyContext getContext() {
        return logicalClass.getContext();
    }

    public Shape getObjectLayout() {
        return dynamicObject.getShape();
    }

    public BasicObjectType getObjectType() {
        return (BasicObjectType) dynamicObject.getShape().getObjectType();
    }

    public RubyClass getLogicalClass() {
        return logicalClass;
    }

    public DynamicObject getDynamicObject() {
        return dynamicObject;
    }

    public static class BasicObjectAllocator implements Allocator {

        // TODO(CS): why on earth is this a boundary? Seems like a really bad thing.
        @TruffleBoundary
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBasicObject(rubyClass);
        }

    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation("RubyBasicObject#toString should only be used for debugging");

        if (RubyGuards.isRubyString(this)) {
            return Helpers.decodeByteList(getContext().getRuntime(), StringNodes.getByteList((this)));
        } else if (RubyGuards.isRubySymbol(this)) {
            return SymbolNodes.getString(this);
        } else if (RubyGuards.isRubyException(this)) {
            return ExceptionNodes.getMessage(this) + " : " + super.toString() + "\n" +
                    Arrays.toString(Backtrace.EXCEPTION_FORMATTER.format(getContext(), this, ExceptionNodes.getBacktrace(this)));
        } else {
            return String.format("RubyBasicObject@%x<logicalClass=%s>", System.identityHashCode(this), logicalClass.getName());
        }
    }

    @TruffleBoundary
    public static void setInstanceVariable(RubyBasicObject receiver, Object name, Object value) {
        Shape shape = receiver.getDynamicObject().getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            property.setGeneric(receiver.getDynamicObject(), value, null);
        } else {
            receiver.getDynamicObject().define(name, value, 0);
        }
    }

    @TruffleBoundary
    public static void setInstanceVariables(RubyBasicObject receiver, Map<Object, Object> instanceVariables) {
        for (Map.Entry<Object, Object> entry : instanceVariables.entrySet()) {
            setInstanceVariable(receiver, entry.getKey(), entry.getValue());
        }
    }

    @TruffleBoundary
    public static Object getInstanceVariable(RubyBasicObject receiver, Object name) {
        Shape shape = receiver.getDynamicObject().getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            return property.get(receiver.getDynamicObject(), false);
        } else {
            return receiver.getContext().getCoreLibrary().getNilObject();
        }
    }

    @TruffleBoundary
    public static Map<Object, Object> getInstanceVariables(RubyBasicObject receiver) {
        Shape shape = receiver.getDynamicObject().getShape();
        Map<Object, Object> vars = new LinkedHashMap<>();
        List<Property> properties = shape.getPropertyList();
        for (Property property : properties) {
            vars.put((String) property.getKey(), property.get(receiver.getDynamicObject(), false));
        }
        return vars;
    }

    @TruffleBoundary
    public static Object[] getFieldNames(RubyBasicObject receiver) {
        List<Object> keys = receiver.getDynamicObject().getShape().getKeyList();
        return keys.toArray(new Object[keys.size()]);
    }

    @TruffleBoundary
    public static boolean isFieldDefined(RubyBasicObject receiver, String name) {
        return receiver.getDynamicObject().getShape().hasProperty(name);
    }

}
