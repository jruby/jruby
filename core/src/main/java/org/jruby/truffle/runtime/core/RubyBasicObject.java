/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents the Ruby {@code BasicObject} class - the root of the Ruby class hierarchy.
 */
public class RubyBasicObject extends ObjectStorage {

    /** The class of the object, not a singleton class. */
    @CompilationFinal protected RubyClass logicalClass;
    /** Either the singleton class if it exists or the logicalClass. */
    @CompilationFinal protected RubyClass metaClass;

    protected long objectID = -1;
    public boolean hasPrivateLayout = false;

    public RubyBasicObject(RubyClass rubyClass) {
        super(rubyClass != null ? rubyClass.getObjectLayoutForInstances() : ObjectLayout.EMPTY);

        if (rubyClass != null) {
            unsafeSetLogicalClass(rubyClass);
        }
    }

    public boolean hasNoSingleton() {
        return false;
    }

    public boolean hasClassAsSingleton() {
        return false;
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

        metaClass = new RubyClass(currentNode, null, logicalClass,
                String.format("#<Class:#<%s:0x%x>>", logicalClass.getName(), getObjectID()), true);

        return metaClass;
    }

    public void setInstanceVariable(String name, Object value) {
        RubyNode.notDesignedForCompilation();

        updateLayoutToMatchClass();

        setField(name, value);

        if (logicalClass.getObjectLayoutForInstances() != objectLayout) {
            logicalClass.setObjectLayoutForInstances(objectLayout);
        }
    }

    public long getObjectID() {
        RubyNode.notDesignedForCompilation();

        if (objectID == -1) {
            objectID = getContext().getNextObjectID();
        }

        return objectID;
    }

    public void setInstanceVariables(Map<String, Object> instanceVariables) {
        RubyNode.notDesignedForCompilation();

        assert instanceVariables != null;
        updateLayoutToMatchClass();
        setFields(instanceVariables);
    }

    public void updateLayoutToMatchClass() {
        RubyNode.notDesignedForCompilation();

        if (objectLayout != logicalClass.getObjectLayoutForInstances()) {
            changeLayout(logicalClass.getObjectLayoutForInstances());
        }
    }

    public void switchToPrivateLayout() {
        RubyNode.notDesignedForCompilation();

        final Map<String, Object> instanceVariables = getFields();

        hasPrivateLayout = true;
        objectLayout = ObjectLayout.EMPTY;

        for (Entry<String, Object> entry : instanceVariables.entrySet()) {
            objectLayout = objectLayout.withNewVariable(entry.getKey(), entry.getValue().getClass());
        }

        setInstanceVariables(instanceVariables);
    }

    public void extend(RubyModule module, RubyNode currentNode) {
        RubyNode.notDesignedForCompilation();

        getSingletonClass(currentNode).include(currentNode, module);
    }

    public void unsafeSetLogicalClass(RubyClass newLogicalClass) {
        assert logicalClass == null;
        logicalClass = newLogicalClass;
        metaClass = newLogicalClass;
    }

    //public void unsafeSetMetaClass(RubyClass newMetaClass) {
    //    assert metaClass == null;
    //    metaClass = newMetaClass;
    //}

    public Object getInstanceVariable(String name) {
        RubyNode.notDesignedForCompilation();

        final Object value = getField(name);

        if (value == null) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            return value;
        }
    }

    public boolean hasPrivateLayout() {
        return hasPrivateLayout;
    }

    public boolean isTrue() {
        return true;
    }

    public void visitObjectGraph(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        if (visitor.visit(this)) {
            metaClass.visitObjectGraph(visitor);

            for (Object instanceVariable : getFields().values()) {
                getContext().getCoreLibrary().box(instanceVariable).visitObjectGraph(visitor);
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

    public RubyClass getLogicalClass() {
        return logicalClass;
    }

}
