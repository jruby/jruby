/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.core;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.StringSupport;

public class StringGuards {

    public static boolean isSingleByteOptimizable(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringSupport.isSingleByteOptimizable(StringNodes.getCodeRangeable(string), StringNodes.getByteList(string).getEncoding());
    }

    public static boolean isAsciiCompatible(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringNodes.getByteList(string).getEncoding().isAsciiCompatible();
    }

    public static boolean isSingleByteOptimizableOrAsciiOnly(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        // TODO (nirvdrnum 08-Jun-15) Rubinius tracks whether a String is ASCII-only via a field in the String.
        return isSingleByteOptimizable(string);
    }

    public static boolean isSingleByte(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringNodes.getByteList(string).getEncoding().isSingleByte();
    }

    public static boolean isValidOr7BitEncoding(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringNodes.isCodeRangeValid(string) || CodeRangeSupport.isCodeRangeAsciiOnly(StringNodes.getCodeRangeable(string));
    }

    public static boolean isFixedWidthEncoding(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringNodes.getByteList(string).getEncoding().isFixedWidth();
    }

    public static boolean isValidUtf8(RubyBasicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringNodes.isCodeRangeValid(string) && StringNodes.getByteList(string).getEncoding() instanceof UTF8Encoding;
    }
}
