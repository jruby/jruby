/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.om.dsl.processor.layout.model;

import org.jruby.truffle.om.dsl.processor.layout.model.PropertyModel;

import javax.lang.model.type.TypeMirror;

public class PropertyBuilder {

    private final String name;
    private boolean hasGetter;
    private boolean hasSetter;
    private TypeMirror type;

    public PropertyBuilder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PropertyModel build() {
        return new PropertyModel(name, hasGetter, hasSetter, type);
    }

    public void setHasGetter(boolean hasGetter) {
        this.hasGetter = hasGetter;
    }

    public void setHasSetter(boolean hasSetter) {
        this.hasSetter = hasSetter;
    }

    public TypeMirror getType() {
        return type;
    }

    public void setType(TypeMirror type) {
        this.type = type;
    }
}
