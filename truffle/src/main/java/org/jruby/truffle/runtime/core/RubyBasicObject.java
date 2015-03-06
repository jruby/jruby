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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyOperations;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.Map;

/**
 * Represents the Ruby {@code BasicObject} class - the root of the Ruby class hierarchy.
 */
public class RubyBasicObject {

    public static final HiddenKey OBJECT_ID_IDENTIFIER = new HiddenKey("object_id");
    public static final HiddenKey TAINTED_IDENTIFIER = new HiddenKey("tainted?");
    public static final HiddenKey FROZEN_IDENTIFIER = new HiddenKey("frozen?");

    public static final Layout LAYOUT = Layout.createLayout(Layout.INT_TO_LONG);

    private final DynamicObject dynamicObject;

    /** The class of the object, not a singleton class. */
    @CompilationFinal protected RubyClass logicalClass;
    /** Either the singleton class if it exists or the logicalClass. */
    @CompilationFinal protected RubyClass metaClass;

    public RubyBasicObject(RubyClass rubyClass) {
        this(rubyClass, rubyClass.getContext());
    }

    public RubyBasicObject(RubyClass rubyClass, RubyContext context) {
        dynamicObject = LAYOUT.newInstance(context.getEmptyShape());

        if (rubyClass != null) {
            unsafeSetLogicalClass(rubyClass);
        }
    }

    protected void unsafeSetLogicalClass(RubyClass newLogicalClass) {
        assert logicalClass == null;
        logicalClass = newLogicalClass;
        metaClass = newLogicalClass;
    }

    public boolean hasNoSingleton() {
        return false;
    }

    public boolean hasClassAsSingleton() {
        return false;
    }

    @Deprecated
    public void freeze() {
        DebugOperations.verySlowFreeze(this);
    }

    @Deprecated
    public void checkFrozen(Node currentNode) {
        if (DebugOperations.verySlowIsFrozen(this)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(getLogicalClass().getName(), currentNode));
        }
    }

    public RubyClass getMetaClass() {
        return metaClass;
    }

    public RubyClass getSingletonClass(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        if (hasNoSingleton()) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorCantDefineSingleton(currentNode));
        }

        if (hasClassAsSingleton() || metaClass.isSingleton()) {
            return metaClass;
        }

        final RubyClass logicalClass = metaClass;

        metaClass = RubyClass.createSingletonClassOfObject(getContext(), logicalClass,
                String.format("#<Class:#<%s:0x%x>>", logicalClass.getName(), getObjectID()));

        if (DebugOperations.verySlowIsFrozen(this)) {
            DebugOperations.verySlowFreeze(metaClass);
        }

        return metaClass;
    }

    @CompilerDirectives.TruffleBoundary
    public long getObjectID() {
        // TODO(CS): we should specialise on reading this in the #object_id method and anywhere else it's used
        Property property = dynamicObject.getShape().getProperty(OBJECT_ID_IDENTIFIER);

        if (property != null) {
            return (long) property.get(dynamicObject, false);
        }

        final long objectID = getContext().getNextObjectID();
        dynamicObject.define(OBJECT_ID_IDENTIFIER, objectID, 0);
        return objectID;
    }

    @CompilerDirectives.TruffleBoundary
    public void setInstanceVariables(Map<Object, Object> instanceVariables) {
        RubyNode.notDesignedForCompilation("008fbda1e1084278939d97d227a7020a");

        assert instanceVariables != null;

        getOperations().setInstanceVariables(this, instanceVariables);
    }


    public Map<Object, Object>  getInstanceVariables() {
        RubyNode.notDesignedForCompilation("738af770e8054dda87dd0937c8c3fa34");

        return getOperations().getInstanceVariables(this);
    }

    public Object[] getFieldNames() {
        return getOperations().getFieldNames(this);
    }

    public void extend(RubyModule module, Node currentNode) {
        RubyNode.notDesignedForCompilation("1c82267e10064ec2a55f86cd86598e1e");
        getSingletonClass(currentNode).include(currentNode, module);
    }

    public Object getInstanceVariable(String name) {
        RubyNode.notDesignedForCompilation("aaf61c62c2c44a559e86853549f73397");

        final Object value = getOperations().getInstanceVariable(this, name);

        if (value == null) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return value;
        }
    }

    public boolean isFieldDefined(String name) {
        return getOperations().isFieldDefined(this, name);
    }

    public final void visitObjectGraph(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (visitor.visit(this)) {
            metaClass.visitObjectGraph(visitor);

            for (Object instanceVariable : getOperations().getInstanceVariables(this).values()) {
                if (instanceVariable instanceof RubyBasicObject) {
                    ((RubyBasicObject) instanceVariable).visitObjectGraph(visitor);
                }
            }

            visitObjectGraphChildren(visitor);
        }
    }

    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
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

    public RubyOperations getOperations() {
        return (RubyOperations) dynamicObject.getShape().getObjectType();
    }

    public RubyClass getLogicalClass() {
        return logicalClass;
    }

    public DynamicObject getDynamicObject() {
        return dynamicObject;
    }

    public static class BasicObjectAllocator implements Allocator {

        // TODO(CS): why on earth is this a boundary? Seems like a really bad thing.
        @CompilerDirectives.TruffleBoundary
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBasicObject(rubyClass);
        }

    }

}
