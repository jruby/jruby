/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.object.*;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RubyOperations extends ObjectType {

    private final RubyContext context;

    public RubyOperations(RubyContext context) {
        this.context = context;
    }

    public void setInstanceVariable(RubyBasicObject receiver, String name, Object value) {
        Shape shape = receiver.getDynamicObject().getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            property.setGeneric(receiver.getDynamicObject(), value, null);
        } else {
            receiver.getDynamicObject().define(name, value, 0);
        }
    }

    public void setInstanceVariables(RubyBasicObject receiver, Map<String, Object> instanceVariables) {
        for (Map.Entry<String, Object> entry : instanceVariables.entrySet()) {
            setInstanceVariable(receiver, entry.getKey(), entry.getValue());
        }
    }

    public Object getInstanceVariable(RubyBasicObject receiver, String name) {
        Shape shape = receiver.getDynamicObject().getShape();
        Property property = shape.getProperty(name);
        if (property != null) {
            return property.get(receiver.getDynamicObject(), false);
        } else {
            return context.getCoreLibrary().getNilObject();
        }
    }

    public Map<String,Object> getInstanceVariables(RubyBasicObject receiver) {
        Shape shape = receiver.getDynamicObject().getShape();
        Map<String, Object> vars = new LinkedHashMap<>();
        List<Property> properties = shape.getPropertyList();
        for (Property property : properties) {
            if (property.getKey() != RubyBasicObject.OBJECT_ID_IDENTIFIER) {
                vars.put((String) property.getKey(), property.get(receiver.getDynamicObject(), false));
            }
        }
        return vars;
    }

    public String[] getFieldNames(RubyBasicObject receiver) {
        return receiver.getDynamicObject().getShape().getKeyList().toArray(new String[0]);
    }

    public boolean isFieldDefined(RubyBasicObject receiver, String name) {
        return receiver.getDynamicObject().getShape().hasProperty(name);
    }

    @Override
    public boolean equals(DynamicObject dynamicObject, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode(DynamicObject dynamicObject) {
        throw new UnsupportedOperationException();
    }

}
