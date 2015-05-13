/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.object;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RubyObjectType extends ObjectType {

    @CompilerDirectives.TruffleBoundary
    public void setInstanceVariable(RubyBasicObject receiver, Object name, Object value) {
        Shape shape = receiver.getDynamicObject().getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            property.setGeneric(receiver.getDynamicObject(), value, null);
        } else {
            receiver.getDynamicObject().define(name, value, 0);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public void setInstanceVariables(RubyBasicObject receiver, Map<Object, Object> instanceVariables) {
        for (Map.Entry<Object, Object> entry : instanceVariables.entrySet()) {
            setInstanceVariable(receiver, entry.getKey(), entry.getValue());
        }
    }

    @CompilerDirectives.TruffleBoundary
    public Object getInstanceVariable(RubyBasicObject receiver, Object name) {
        Shape shape = receiver.getDynamicObject().getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            return property.get(receiver.getDynamicObject(), false);
        } else {
            return receiver.getContext().getCoreLibrary().getNilObject();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public Map<Object, Object> getInstanceVariables(RubyBasicObject receiver) {
        Shape shape = receiver.getDynamicObject().getShape();
        Map<Object, Object> vars = new LinkedHashMap<>();
        List<Property> properties = shape.getPropertyList();
        for (Property property : properties) {
            vars.put((String) property.getKey(), property.get(receiver.getDynamicObject(), false));
        }
        return vars;
    }

    @CompilerDirectives.TruffleBoundary
    public Object[] getFieldNames(RubyBasicObject receiver) {
        List<Object> keys = receiver.getDynamicObject().getShape().getKeyList();
        return keys.toArray(new Object[keys.size()]);
    }

    @CompilerDirectives.TruffleBoundary
    public boolean isFieldDefined(RubyBasicObject receiver, String name) {
        return receiver.getDynamicObject().getShape().hasProperty(name);
    }

}
