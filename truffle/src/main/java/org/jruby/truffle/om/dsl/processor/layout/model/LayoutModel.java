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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LayoutModel {

    private final String name;
    private final String packageName;
    private final String interfaceFullName;
    private final boolean hasObjectGuard;
    private final boolean hasDynamicObjectGuard;
    private final List<PropertyModel> properties;

    public LayoutModel(String name, String packageName, boolean hasObjectGuard, boolean hasDynamicObjectGuard, Collection<PropertyModel> properties, String interfaceFullName) {
        this.name = name;
        this.packageName = packageName;
        this.interfaceFullName = interfaceFullName;
        this.hasObjectGuard = hasObjectGuard;
        this.hasDynamicObjectGuard = hasDynamicObjectGuard;
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getNameAsConstant() {
        return name.toUpperCase();
    }

    public String getInterfaceFullName() {
        return interfaceFullName;
    }

    public boolean hasObjectGuard() {
        return hasObjectGuard;
    }

    public boolean hasDynamicObjectGuard() {
        return hasDynamicObjectGuard;
    }

    public List<PropertyModel> getProperties() {
        return properties;
    }

}
