/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.util.Locale;

public abstract class StringUtils {

    @TruffleBoundary
    public static String create(byte[] bytes) {
        return new String(bytes);
    }

    @TruffleBoundary
    public static String format(Locale locale, String format, Object... args) {
        return String.format(locale, format, args);
    }

    @TruffleBoundary
    public static String format(String format, Object... args) {
        return String.format(format, args);
    }

    @TruffleBoundary
    public static String replace(String string, char oldChar, char newChar) {
        return string.replace(oldChar, newChar);
    }

    @TruffleBoundary
    public static String replace(String string, CharSequence target, CharSequence replacement) {
        return string.replace(target, replacement);
    }

    @TruffleBoundary
    public static String toLowerCase(String string) {
        return string.toLowerCase(Locale.ENGLISH);
    }

}
