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

import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.hash.Entry;

public abstract class HashGuards {

    // Storage strategies

    public static boolean isNullHash(RubyBasicObject hash) {
        return HashNodes.getStore(hash) == null;
    }

    public static boolean isPackedHash(RubyBasicObject hash) {
        // Can't do instanceof Object[] due to covariance
        return !(isNullHash(hash) || isBucketHash(hash));
    }

    public static boolean isBucketHash(RubyBasicObject hash) {
        return HashNodes.getStore(hash) instanceof Entry[];
    }

    // Higher level properties

    public static boolean isEmptyHash(RubyBasicObject hash) {
        return HashNodes.getSize(hash) == 0;
    }

    public static boolean isCompareByIdentity(RubyBasicObject hash) {
        return HashNodes.isCompareByIdentity(hash);
    }

    public static boolean hasDefaultValue(RubyBasicObject hash) {
        return HashNodes.getDefaultValue(hash) != null;
    }

    public static boolean hasDefaultBlock(RubyBasicObject hash) {
        return HashNodes.getDefaultBlock(hash) != null;
    }

}
