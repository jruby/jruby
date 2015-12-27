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
    private final boolean hasObjectTypeGetter;
    private final boolean hasFactoryGetter;
    private final boolean hasFactorySetter;
    private final boolean hasGetter;
    private final boolean hasSetter;
    private final boolean hasUnsafeSetter;
    private final TypeMirror type;
    private final boolean nullable;
    private final boolean volatileSemantics;
    private final boolean hasCompareAndSet;
    private final boolean hasGetAndSet;
    private final boolean hasIdentifier;
    private final boolean isShapeProperty;

    public PropertyModel(String name, boolean hasObjectTypeGetter, boolean hasFactoryGetter, boolean hasFactorySetter,
                         boolean hasGetter, boolean hasSetter, boolean hasUnsafeSetter,
                         TypeMirror type, boolean nullable,
                         boolean volatileSemantics, boolean hasCompareAndSet, boolean hasGetAndSet,
                         boolean hasIdentifier, boolean isShapeProperty) {
        // assert name != null;
        // assert type != null;

        //if (hasFactoryGetter || hasFactorySetter || hasObjectTypeGetter) {
        //    assert isShapeProperty;
        //}

        // assert !(volatileSemantics && isShapeProperty);
        // assert !(volatileSemantics && hasUnsafeSetter);

        this.name = name;
        this.hasObjectTypeGetter = hasObjectTypeGetter;
        this.hasFactoryGetter = hasFactoryGetter;
        this.hasFactorySetter = hasFactorySetter;
        this.hasGetter = hasGetter;
        this.hasSetter = hasSetter;
        this.hasUnsafeSetter = hasUnsafeSetter;
        this.type = type;
        this.nullable = nullable;
        this.volatileSemantics = volatileSemantics;
        this.hasCompareAndSet = hasCompareAndSet;
        this.hasGetAndSet = hasGetAndSet;
        this.hasIdentifier = hasIdentifier;
        this.isShapeProperty = isShapeProperty;
    }

    public String getName() {
        return name;
    }

    public boolean hasObjectTypeGetter() {
        return hasObjectTypeGetter;
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

    public boolean isFinal() {
        return !(hasSetter || hasGetAndSet || hasCompareAndSet);
    }

    public boolean hasUnsafeSetter() {
        return hasUnsafeSetter;
    }

    public TypeMirror getType() {
        return type;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isVolatile() {
        return volatileSemantics;
    }

    public boolean hasCompareAndSet() {
        return hasCompareAndSet;
    }

    public boolean hasGetAndSet() {
        return hasGetAndSet;
    }

    public boolean hasIdentifier() {
        return hasIdentifier;
    }

    public boolean isShapeProperty() {
        return isShapeProperty;
    }
}
