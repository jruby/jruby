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

import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.StringSupport;

public class StringGuards {

    public static boolean isSingleByteOptimizable(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringSupport.isSingleByteOptimizable(StringOperations.getCodeRangeable(string), Layouts.STRING.getByteList(string).getEncoding());
    }

    public static boolean isAsciiCompatible(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return Layouts.STRING.getByteList(string).getEncoding().isAsciiCompatible();
    }

    public static boolean isSingleByteOptimizableOrAsciiOnly(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        // TODO (nirvdrnum 08-Jun-15) Rubinius tracks whether a String is ASCII-only via a field in the String.
        return isSingleByteOptimizable(string);
    }

    public static boolean isSingleByte(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return Layouts.STRING.getByteList(string).getEncoding().isSingleByte();
    }

    public static boolean isValidOr7BitEncoding(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringOperations.isCodeRangeValid(string) || CodeRangeSupport.isCodeRangeAsciiOnly(StringOperations.getCodeRangeable(string));
    }

    public static boolean isFixedWidthEncoding(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return Layouts.STRING.getByteList(string).getEncoding().isFixedWidth();
    }

    public static boolean isValidUtf8(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return StringOperations.isCodeRangeValid(string) && Layouts.STRING.getByteList(string).getEncoding() instanceof UTF8Encoding;
    }
}
