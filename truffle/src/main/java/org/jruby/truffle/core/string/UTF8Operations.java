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

public abstract class UTF8Operations {

    public static boolean isUTF8ValidOneByte(byte b) {
        return b >= 0;
    }

    public static boolean isUTF8ValidTwoBytes(byte... bytes) {
        assert bytes.length == 2;

        if ((bytes[0] & 0xff) >= 0xc2 && (bytes[0] & 0xff) <= 0xdf) {
            return (bytes[1] & 0xff) >= 0x80 && (bytes[1] & 0xff) <= 0xbf;
        }

        return false;
    }

    public static boolean isUTF8ValidThreeBytes(byte... bytes) {
        assert bytes.length == 3;

        if ((bytes[0] & 0xff) < 0xe0 || (bytes[0] & 0xff) > 0xef) {
            return false;
        }

        if ((bytes[2] & 0xff) < 0x80 || (bytes[2] & 0xff) > 0xbf) {
            return false;
        }

        if ((bytes[1] & 0xff) >= 0x80 || (bytes[2] & 0xff) <= 0xbf) {
            if ((bytes[0] & 0xff) == 0xe0) {
                return (bytes[1] & 0xff) >= 0xa0;
            }

            if ((bytes[0] & 0xff) == 0xed) {
                return (bytes[1] & 0xff) <= 0x9f;
            }

            return true;
        }

        return false;
    }

    public static boolean isUTF8ValidFourBytes(byte... bytes) {
        assert bytes.length == 4;

        if ((bytes[3] & 0xff) < 0x80 || (bytes[3] & 0xff) > 0xbf) {
            return false;
        }

        if ((bytes[2] & 0xff) < 0x80 || (bytes[2] & 0xff) > 0xbf) {
            return false;
        }

        if ((bytes[0] & 0xff) < 0xf0 || (bytes[0] & 0xff) > 0xf4) {
            return false;
        }

        if ((bytes[1] & 0xff) >= 0x80 || (bytes[2] & 0xff) <= 0xbf) {
            if ((bytes[0] & 0xff) == 0xf0) {
                return (bytes[1] & 0xff) >= 0x90;
            }

            if ((bytes[0] & 0xff) == 0xf4) {
                return (bytes[1] & 0xff) <= 0x8f;
            }

            return true;
        }

        return false;
    }

    public static boolean isUTF8ValidFiveBytes(byte... bytes) {
        assert bytes.length == 5;

        // There are currently no valid five byte UTF-8 codepoints.
        return false;
    }

    public static boolean isUTF8ValidSixBytes(byte... bytes) {
        assert bytes.length == 6;

        // There are currently no valid six byte UTF-8 codepoints.
        return false;
    }

}
