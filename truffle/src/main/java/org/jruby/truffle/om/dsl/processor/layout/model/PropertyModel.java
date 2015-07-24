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

import javax.lang.model.type.TypeMirror;

public class PropertyModel {

    private final String name;
    private final boolean hasGetter;
    private final boolean hasSetter;
    private final TypeMirror type;
    private final boolean nullable;

    public PropertyModel(String name, boolean hasGetter, boolean hasSetter, TypeMirror type, boolean nullable) {
        this.name = name;
        this.hasGetter = hasGetter;
        this.hasSetter = hasSetter;
        this.type = type;
        this.nullable = nullable;
    }

    public String getName() {
        return name;
    }

    public String getNameAsConstant() {
        return name.toUpperCase();
    }

    public String getNameAsGetter() {
        return getNameAsGetterSetter("get");
    }

    public String getNameAsSetter() {
        return getNameAsGetterSetter("set");
    }

    private String getNameAsGetterSetter(String getSet) {
        return getSet + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public boolean hasGetter() {
        return hasGetter;
    }

    public boolean hasSetter() {
        return hasSetter;
    }

    public TypeMirror getType() {
        return type;
    }

    public boolean isNullable() {
        return nullable;
    }
}
