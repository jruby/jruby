/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.hash;

import org.jruby.util.cli.Options;

public abstract class PackedArrayStrategy {

    public static final int TRUFFLE_HASH_PACKED_ARRAY_MAX = Options.TRUFFLE_HASH_PACKED_ARRAY_MAX.load();

    public static Object getKey(Object[] store, int n) {
        return store[n * 2];
    }

    public static Object getValue(Object[] store, int n) {
        return store[n * 2 + 1];
    }

    public static void setKey(Object[] store, int n, Object key) {
        store[n * 2] = key;
    }

    public static void setValue(Object[] store, int n, Object value) {
        store[n * 2 + 1] = value;
    }

    public static void setKeyValue(Object[] store, int n, Object key, Object value) {
        setKey(store, n, key);
        setValue(store, n, value);
    }

}
