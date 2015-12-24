/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;

public abstract class StringCachingGuards {

    public static ByteList privatizeByteList(DynamicObject string) {
        if (RubyGuards.isRubyString(string)) {
            return StringOperations.getByteList(string).dup();
        } else {
            return null;
        }
    }

    public static boolean byteListsEqual(DynamicObject string, ByteList byteList) {
        if (RubyGuards.isRubyString(string)) {
            // TODO CS 8-Nov-15 this code goes off into the woods - need to break it apart and branch profile it
            return StringOperations.getByteList(string).equal(byteList);
        } else {
            return false;
        }
    }

    public static int byteListLength(ByteList byteList) {
        return byteList.length();
    }

}
