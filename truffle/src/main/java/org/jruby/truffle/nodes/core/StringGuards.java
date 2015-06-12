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
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.StringSupport;

public class StringGuards {

    public static boolean isSingleByteOptimizable(RubyString string) {
        return StringSupport.isSingleByteOptimizable(StringNodes.getCodeRangeable(string), StringNodes.getByteList(string).getEncoding());
    }

    public static boolean isAsciiCompatible(RubyString string) {
        return StringNodes.getByteList(string).getEncoding().isAsciiCompatible();
    }

    public static boolean isSingleByteOptimizableOrAsciiOnly(RubyString string) {
        // TODO (nirvdrnum 08-Jun-15) Rubinius tracks whether a String is ASCII-only via a field in the String.
        return isSingleByteOptimizable(string);
    }

    public static boolean isSingleByte(RubyString string) {
        return StringNodes.getByteList(string).getEncoding().isSingleByte();
    }

    public static boolean isValidOr7BitEncoding(RubyString string) {
        return StringNodes.isCodeRangeValid(string) || CodeRangeSupport.isCodeRangeAsciiOnly(StringNodes.getCodeRangeable(string));
    }

    public static boolean isFixedWidthEncoding(RubyString string) {
        return StringNodes.getByteList(string).getEncoding().isFixedWidth();
    }

    public static boolean isValidUtf8(RubyString string) {
        return StringNodes.isCodeRangeValid(string) && StringNodes.getByteList(string).getEncoding() instanceof UTF8Encoding;
    }
}
