/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.hash.Entry;

public class HashGuards {

    public static boolean isNull(RubyHash hash) {
        return hash.getStore() == null;
    }

    public static boolean isBuckets(RubyHash hash) {
        return hash.getStore() instanceof Entry[];
    }
    
    public static boolean isCompareByIdentity(RubyHash hash) {
        return hash.isCompareByIdentity();
    }
    
    public static boolean isEmpty(RubyHash hash) {
        return hash.getSize() == 0;
    }

    public static boolean hasDefaultValue(RubyHash hash) {
        return hash.getDefaultValue() != null;
    }

    public static boolean hasDefaultBlock(RubyHash hash) {
        return hash.getDefaultBlock() != null;
    }

}
