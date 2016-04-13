/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GlobalVariables {

    private final DynamicObject defaultValue;

    ConcurrentMap<String, GlobalVariableStorage> variables;

    public GlobalVariables(DynamicObject defaultValue) {
        this.defaultValue = defaultValue;
        this.variables = new ConcurrentHashMap<>();
    }

    public Object getOrDefault(String key, Object defaultValue) {
        final Object v;
        return ((v = get(key)) != null) ? v : defaultValue;
    }

    public Object get(String key) {
        return getStorage(key).value;
    }

    @TruffleBoundary
    public GlobalVariableStorage getStorage(String key) {
        final GlobalVariableStorage currentStorage = variables.get(key);
        if (currentStorage == null) {
            final GlobalVariableStorage newStorage = new GlobalVariableStorage(defaultValue);
            final GlobalVariableStorage racyNewStorage = variables.putIfAbsent(key, newStorage);
            return (racyNewStorage == null) ? newStorage : racyNewStorage;
        } else {
            return currentStorage;
        }
    }

    public void put(String key, Object value) {
        getStorage(key).value = value;
    }

    public Collection<DynamicObject> dynamicObjectValues() {
        final Collection<GlobalVariableStorage> storages = variables.values();
        final ArrayList<DynamicObject> values = new ArrayList<>(storages.size());
        for (GlobalVariableStorage storage : storages) {
            final Object value = storage.value;
            if (value instanceof DynamicObject) {
                values.add((DynamicObject) value);
            }
        }
        return values;
    }

}
