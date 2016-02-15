/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.string;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.language.RubyGuards;

public abstract class StringCachingGuards {

    public static Rope privatizeRope(DynamicObject string) {
        if (RubyGuards.isRubyString(string)) {
            // TODO (nirvdrum 25-Jan-16) Should we flatten the rope to avoid caching a potentially deep rope tree?
            return StringOperations.rope(string);
        } else {
            return null;
        }
    }

    public static boolean ropesEqual(DynamicObject string, Rope rope) {
        if (RubyGuards.isRubyString(string)) {
            final Rope stringRope = StringOperations.rope(string);

            // equal below does not check encoding
            if (stringRope.getEncoding() != rope.getEncoding()) {
                return false;
            }

            return stringRope.equals(rope);
        } else {
            return false;
        }
    }

    public static int ropeLength(Rope rope) {
        return rope.byteLength();
    }

}
