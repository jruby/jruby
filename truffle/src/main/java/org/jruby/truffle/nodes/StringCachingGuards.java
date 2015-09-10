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
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;

public abstract class StringCachingGuards {

    public static ByteList privatizeByteList(DynamicObject string) {
        if (RubyGuards.isRubyString(string)) {
            return Layouts.STRING.getByteList(string).dup();
        } else {
            return null;
        }
    }

    public static boolean byteListsEqual(DynamicObject string, ByteList byteList) {
        if (RubyGuards.isRubyString(string)) {
            return Layouts.STRING.getByteList(string).equal(byteList);
        } else {
            return false;
        }
    }

}
