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
    private final boolean hasFactoryGetter;
    private final boolean hasFactorySetter;
    private final boolean hasGetter;
    private final boolean hasSetter;
    private final TypeMirror type;
    private final boolean nullable;
    private final boolean hasIdentifier;
    private final boolean isShapeProperty;

    public PropertyModel(String name, boolean hasFactoryGetter, boolean hasFactorySetter,
                         boolean hasGetter, boolean hasSetter,
                         TypeMirror type, boolean nullable, boolean hasIdentifier,
                         boolean isShapeProperty) {
        assert name != null;
        assert type != null;

        if (hasFactoryGetter || hasFactorySetter) {
            assert isShapeProperty;
        }

        this.name = name;
        this.hasFactoryGetter = hasFactoryGetter;
        this.hasFactorySetter = hasFactorySetter;
        this.hasGetter = hasGetter;
        this.hasSetter = hasSetter;
        this.type = type;
        this.nullable = nullable;
        this.hasIdentifier = hasIdentifier;
        this.isShapeProperty = isShapeProperty;
    }

    public String getName() {
        return name;
    }

    public boolean hasFactorySetter() {
        return hasFactorySetter;
    }

    public boolean hasFactoryGetter() {
        return hasFactoryGetter;
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

    public boolean hasIdentifier() {
        return hasIdentifier;
    }

    public boolean isShapeProperty() {
        return isShapeProperty;
    }
}
