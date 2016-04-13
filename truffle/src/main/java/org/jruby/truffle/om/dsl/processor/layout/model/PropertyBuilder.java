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

public class PropertyBuilder {

    private final String name;
    private boolean hasObjectTypeGetter;
    private boolean hasFactoryGetter;
    private boolean hasFactorySetter;
    private boolean hasGetter;
    private boolean hasSetter;
    private boolean hasUnsafeSetter;
    private TypeMirror type;
    private boolean nullable;
    private boolean volatileSemantics;
    private boolean hasCompareAndSet;
    private boolean hasGetAndSet;
    private boolean hasIdentifier;
    private boolean isShapeProperty;

    public PropertyBuilder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PropertyModel build() {
        // assert type != null;

        return new PropertyModel(name, hasObjectTypeGetter, hasFactoryGetter, hasFactorySetter,
                hasGetter, hasSetter, hasUnsafeSetter, type,
                nullable, volatileSemantics, hasCompareAndSet, hasGetAndSet, hasIdentifier, isShapeProperty);
    }

    public void setHasObjectTypeGetter(boolean hasObjectTypeGetter) {
        this.hasObjectTypeGetter = hasObjectTypeGetter;
    }

    public void setHasFactoryGetter(boolean hasFactoryGetter) {
        this.hasFactoryGetter = hasFactoryGetter;
    }

    public void setHasFactorySetter(boolean hasFactorySetter) {
        this.hasFactorySetter = hasFactorySetter;
    }

    public void setHasGetter(boolean hasGetter) {
        this.hasGetter = hasGetter;
    }

    public void setHasSetter(boolean hasSetter) {
        this.hasSetter = hasSetter;
    }

    public void setHasUnsafeSetter(boolean hasUnsafeSetter) {
        this.hasUnsafeSetter = hasUnsafeSetter;
    }

    public TypeMirror getType() {
        return type;
    }

    public void setType(TypeMirror type) {
        this.type = type;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public void setVolatile(boolean volatileSemantics) {
        this.volatileSemantics = volatileSemantics;
    }

    public void setHasCompareAndSet(boolean hasCompareAndSet) {
        // assert !hasCompareAndSet || volatileSemantics;
        this.hasCompareAndSet = hasCompareAndSet;
    }

    public void setHasGetAndSet(boolean hasGetAndSet) {
        // assert !hasGetAndSet || volatileSemantics;
        this.hasGetAndSet = hasGetAndSet;
    }

    public void setHasIdentifier(boolean hasIdentifier) {
        this.hasIdentifier = hasIdentifier;
    }

    public boolean isShapeProperty() {
        return isShapeProperty;
    }

    public void setIsShapeProperty(boolean isShapeProperty) {
        this.isShapeProperty = isShapeProperty;
    }
}
