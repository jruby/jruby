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
        return StringSupport.isSingleByteOptimizable(string, string.getByteList().getEncoding());
    }

    public static boolean isAsciiCompatible(RubyString string) {
        return string.getByteList().getEncoding().isAsciiCompatible();
    }

    public static boolean isSingleByteOptimizableOrAsciiCompatible(RubyString string) {
        return isSingleByteOptimizable(string) || isAsciiCompatible(string);
    }

    public static boolean isSingleByte(RubyString string) {
        return string.getByteList().getEncoding().isSingleByte();
    }

    public static boolean isValidOr7BitEncoding(RubyString string) {
        return string.isCodeRangeValid() || CodeRangeSupport.isCodeRangeAsciiOnly(string);
    }

    public static boolean isFixedWidthEncoding(RubyString string) {
        return string.getByteList().getEncoding().isFixedWidth();
    }

    public static boolean isValidUtf8(RubyString string) {
        return string.isCodeRangeValid() && string.getByteList().getEncoding() instanceof UTF8Encoding;
    }
}
