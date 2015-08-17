/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.hash;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.hash.Entry;

public abstract class HashGuards {

    // Storage strategies

    public static boolean isNullHash(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return HashNodes.HASH_LAYOUT.getStore(hash) == null;
    }

    public static boolean isPackedHash(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        // Can't do instanceof Object[] due to covariance
        return !(isNullHash(hash) || isBucketHash(hash));
    }

    public static boolean isBucketHash(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return HashNodes.HASH_LAYOUT.getStore(hash) instanceof Entry[];
    }

    // Higher level properties

    public static boolean isEmptyHash(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return HashNodes.HASH_LAYOUT.getSize(hash) == 0;
    }

    public static boolean isCompareByIdentity(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return HashNodes.HASH_LAYOUT.getCompareByIdentity(hash);
    }

    public static boolean hasDefaultValue(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return HashNodes.HASH_LAYOUT.getDefaultValue(hash) != null;
    }

    public static boolean hasDefaultBlock(DynamicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return HashNodes.HASH_LAYOUT.getDefaultBlock(hash) != null;
    }

}
