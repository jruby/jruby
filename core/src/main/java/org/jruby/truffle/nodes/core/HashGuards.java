/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import org.jruby.truffle.runtime.core.RubyHash;

import java.util.LinkedHashMap;

public class HashGuards {

    public static boolean isNull(RubyHash hash) {
        return hash.getStore() == null;
    }

    public static boolean isObjectArray(RubyHash hash) {
        return hash.getStore() instanceof Object[];
    }

    public static boolean isObjectLinkedHashMap(RubyHash hash) {
        return hash.getStore() instanceof LinkedHashMap<?, ?>;
    }

    public static boolean isOtherNull(RubyHash hash, RubyHash other) {
        return other.getStore() == null;
    }

    public static boolean isOtherObjectArray(RubyHash hash, RubyHash other) {
        return other.getStore() instanceof Object[];
    }

    public static boolean isOtherObjectLinkedHashMap(RubyHash hash, RubyHash other) {
        return other.getStore() instanceof LinkedHashMap<?, ?>;
    }

}
