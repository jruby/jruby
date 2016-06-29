/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */


package org.jruby.truffle.core.rope;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyGuards;

import static org.jruby.truffle.core.rope.CodeRange.CR_7BIT;
import static org.jruby.truffle.core.string.StringOperations.rope;

public class RopeGuards {

    public static boolean isSingleByteString(Rope rope) {
        return rope.byteLength() == 1;
    }

    public static boolean is7Bit(Rope rope) {
        return rope.getCodeRange() == CR_7BIT;
    }

    public static boolean isRopeBuffer(Rope rope) {
        return rope instanceof RopeBuffer;
    }

    protected boolean isRopeBuffer(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return rope(string) instanceof RopeBuffer;
    }

    public static boolean isLeafRope(Rope rope) {
        return rope instanceof LeafRope;
    }

}
