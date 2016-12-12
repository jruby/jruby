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
    private final ConcurrentMap<String, GlobalVariableStorage> variables = new ConcurrentHashMap<>();

    public GlobalVariables(DynamicObject defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object get(String key) {
        return getStorage(key).getValue();
    }

    @TruffleBoundary
    public GlobalVariableStorage getStorage(String key) {
        return variables.computeIfAbsent(key, k -> new GlobalVariableStorage(defaultValue));
    }

    public GlobalVariableStorage put(String key, Object value) {
        assert !variables.containsKey(key);
        final GlobalVariableStorage storage = new GlobalVariableStorage(value);
        variables.put(key, storage);
        return storage;
    }

    @TruffleBoundary
    public void alias(String name, GlobalVariableStorage storage) {
        variables.put(name, storage);
    }

    @TruffleBoundary
    public Collection<String> keys() {
        return variables.keySet();
    }

    @TruffleBoundary
    public Collection<DynamicObject> dynamicObjectValues() {
        final Collection<GlobalVariableStorage> storages = variables.values();
        final ArrayList<DynamicObject> values = new ArrayList<>(storages.size());
        for (GlobalVariableStorage storage : storages) {
            final Object value = storage.getValue();
            if (value instanceof DynamicObject) {
                values.add((DynamicObject) value);
            }
        }
        return values;
    }

}
