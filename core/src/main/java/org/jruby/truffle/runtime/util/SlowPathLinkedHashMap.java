/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.util;

import com.oracle.truffle.api.CompilerDirectives;

import java.util.LinkedHashMap;

public abstract class SlowPathLinkedHashMap {

    @CompilerDirectives.SlowPath
    public static LinkedHashMap<Object, Object> allocate() {
        return new LinkedHashMap<>();
    }

    @CompilerDirectives.SlowPath
    public static void put(LinkedHashMap<Object, Object> map, Object key, Object value) {
        map.put(key, value);
    }

    @CompilerDirectives.SlowPath
    public static Object get(LinkedHashMap<Object, Object> map, Object key) {
        return map.get(key);
    }

    @CompilerDirectives.SlowPath
    public static int size(LinkedHashMap<Object, Object> map) {
        return map.size();
    }

    @CompilerDirectives.SlowPath
    public static boolean isEmpty(LinkedHashMap<Object, Object> map) {
        return map.isEmpty();
    }

}
