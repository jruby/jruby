/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
/*
 * Copyright (c) 2013, 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyString.java
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 *
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.RubyEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.encoding.EncodingNodes;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;

import java.nio.charset.Charset;

public abstract class StringOperations {

    /** Creates a String from the ByteList, with unknown CR */
    public static DynamicObject createString(RubyContext context, ByteList bytes) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), ropeFromByteList(bytes, CodeRange.CR_UNKNOWN));
    }

    /** Creates a String from the ByteList, with 7-bit CR */
    public static DynamicObject create7BitString(RubyContext context, ByteList bytes) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), ropeFromByteList(bytes, CodeRange.CR_7BIT));
    }

    public static DynamicObject createString(RubyContext context, Rope rope) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), rope);
    }

    // Since ByteList.toString does not decode properly
    @CompilerDirectives.TruffleBoundary
    public static String getString(RubyContext context, DynamicObject string) {
        return RopeOperations.decodeRope(context.getJRubyRuntime(), StringOperations.rope(string));
    }

    public static StringCodeRangeableWrapper getCodeRangeableReadWrite(final DynamicObject string) {
        return new StringCodeRangeableWrapper(string) {
            private final ByteList byteList = RopeOperations.toByteListCopy(StringOperations.rope(string));
            int codeRange = StringOperations.getCodeRange(string).toInt();

            @Override
            public void setCodeRange(int newCodeRange) {
                this.codeRange = newCodeRange;
            }

            @Override
            public int getCodeRange() {
                return codeRange;
            }

            @Override
            public ByteList getByteList() {
                return byteList;
            }
        };
    }

    public static StringCodeRangeableWrapper getCodeRangeableReadOnly(final DynamicObject string) {
        return new StringCodeRangeableWrapper(string) {
            @Override
            public ByteList getByteList() {
                return StringOperations.getByteListReadOnly(string);
            }
        };
    }

    public static CodeRange getCodeRange(DynamicObject string) {
        return Layouts.STRING.getRope(string).getCodeRange();
    }

    public static void setCodeRange(DynamicObject string, int codeRange) {
        // TODO (nirvdrum 07-Jan-16) Code range is now stored in the rope and ropes are immutable -- all calls to this method are suspect.
        final int existingCodeRange = StringOperations.getCodeRange(string).toInt();

        if (existingCodeRange != codeRange) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(String.format("Tried changing the code range value for a rope from %d to %d", existingCodeRange, codeRange));
        }
    }

    public static boolean isCodeRangeValid(DynamicObject string) {
        return StringOperations.getCodeRange(string) == CodeRange.CR_VALID;
    }

    public static void clearCodeRange(DynamicObject string) {
        StringOperations.setCodeRange(string, CodeRange.CR_UNKNOWN.toInt());
    }

    public static void keepCodeRange(DynamicObject string) {
        if (StringOperations.getCodeRange(string) == CodeRange.CR_BROKEN) {
            clearCodeRange(string);
        }
    }

    public static void modify(DynamicObject string) {
        // No-op. Ropes are immutable so any modifications must've been handled elsewhere.
        // TODO (nirvdrum 07-Jan-16) Remove this method once we've inspected each caller for correctness.
    }

    public static void modify(DynamicObject string, int length) {
        // No-op. Ropes are immutable so any modifications must've been handled elsewhere.
        // TODO (nirvdrum 07-Jan-16) Remove this method once we've inspected each caller for correctness.
    }

    public static void modifyAndKeepCodeRange(DynamicObject string) {
        modify(string);
        keepCodeRange(string);
    }

    @TruffleBoundary(throwsControlFlowException = true)
    public static Encoding checkEncoding(DynamicObject string, CodeRangeable other) {
        final Encoding encoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, ((StringCodeRangeableWrapper) other).getString());

        // TODO (nirvdrum 23-Mar-15) We need to raise a proper Truffle+JRuby exception here, rather than a non-Truffle JRuby exception.
        if (encoding == null) {
            final RubyContext context = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(string)).getContext();
            throw context.getJRubyRuntime().newEncodingCompatibilityError(
                    String.format("incompatible character encodings: %s and %s",
                            Layouts.STRING.getRope(string).getEncoding().toString(),
                            other.getByteList().getEncoding().toString()));
        }

        return encoding;
    }

    public static void forceEncodingVerySlow(DynamicObject string, Encoding encoding) {
        final Rope oldRope = Layouts.STRING.getRope(string);
        StringOperations.setRope(string, RopeOperations.withEncodingVerySlow(oldRope, encoding, CodeRange.CR_UNKNOWN));
    }

    public static int normalizeIndex(int length, int index) {
        return ArrayOperations.normalizeIndex(length, index);
    }

    public static int clampExclusiveIndex(DynamicObject string, int index) {
        assert RubyGuards.isRubyString(string);

        // TODO (nirvdrum 21-Jan-16): Verify this is supposed to be the byteLength and not the characterLength.
        return ArrayOperations.clampExclusiveIndex(StringOperations.rope(string).byteLength(), index);
    }

    public static Encoding checkEncoding(RubyContext context, DynamicObject string, DynamicObject other, Node node) {
        final Encoding encoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, other);

        if (encoding == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreExceptions().encodingCompatibilityErrorIncompatible(
                    rope(string).getEncoding().toString(),
                    rope(other).getEncoding().toString(),
                    node));
        }

        return encoding;
    }

    @TruffleBoundary
    public static Rope encodeRope(CharSequence value, Encoding encoding, CodeRange codeRange) {
        // Taken from org.jruby.RubyString#encodeByteList.

        Charset charset = encoding.getCharset();

        // if null charset, fall back on Java default charset
        if (charset == null) charset = Charset.defaultCharset();

        byte[] bytes;
        if (charset == RubyEncoding.UTF8) {
            bytes = RubyEncoding.encodeUTF8(value);
        } else if (charset == RubyEncoding.UTF16) {
            bytes = RubyEncoding.encodeUTF16(value);
        } else {
            bytes = RubyEncoding.encode(value, charset);
        }

        return RopeOperations.create(bytes, encoding, codeRange);
    }

    public static Rope encodeRope(CharSequence value, Encoding encoding) {
        return encodeRope(value, encoding, CodeRange.CR_UNKNOWN);
    }

    @TruffleBoundary
    public static Rope createRope(String s, Encoding encoding) {
        return RopeOperations.create(ByteList.encode(s, "ISO-8859-1"), encoding, CodeRange.CR_UNKNOWN);
    }

    public static ByteList getByteListReadOnly(DynamicObject object) {
        return RopeOperations.getByteListReadOnly(rope(object));
    }

    public static Rope ropeFromByteList(ByteList byteList) {
        return RopeOperations.create(byteList.bytes(), byteList.getEncoding(), CodeRange.CR_UNKNOWN);
    }

    public static Rope ropeFromByteList(ByteList byteList, CodeRange codeRange) {
        // TODO (nirvdrum 08-Jan-16) We need to make a copy of the ByteList's bytes for now to be safe, but we should be able to use the unsafe bytes as we move forward.
        return RopeOperations.create(byteList.bytes(), byteList.getEncoding(), codeRange);
    }

    public static Rope ropeFromByteList(ByteList byteList, int codeRange) {
        // TODO (nirvdrum 08-Jan-16) We need to make a copy of the ByteList's bytes for now to be safe, but we should be able to use the unsafe bytes as we move forward.
        return RopeOperations.create(byteList.bytes(), byteList.getEncoding(), CodeRange.fromInt(codeRange));
    }

    @TruffleBoundary
    public static ByteList createByteList(CharSequence s) {
        return ByteList.create(s);
    }

    public static Rope rope(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return Layouts.STRING.getRope(string);
    }

    public static void setRope(DynamicObject string, Rope rope) {
        assert RubyGuards.isRubyString(string);

        Layouts.STRING.setRope(string, rope);
    }

    public static Encoding encoding(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return rope(string).getEncoding();
    }

    public static CodeRange codeRange(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return rope(string).getCodeRange();
    }

    public static String decodeUTF8(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return RopeOperations.decodeUTF8(Layouts.STRING.getRope(string));
    }

    public static boolean isUTF8ValidOneByte(byte b) {
        return b >= 0;
    }

    public static boolean isUTF8ValidTwoBytes(byte... bytes) {
        assert bytes.length == 2;

        if (bytes[0] >= 0xc2 && bytes[0] <= 0xdf) {
            return bytes[1] >= 0x80 && bytes[1] <= 0xbf;
        }

        return false;
    }

    public static boolean isUTF8ValidThreeBytes(byte... bytes) {
        assert bytes.length == 3;

        if (bytes[0] < 0xe0 || bytes[0] > 0xef) {
            return false;
        }

        if (bytes[2] < 0x80 || bytes[2] > 0xbf) {
            return false;
        }

        if (bytes[1] >= 0x80 || bytes[2] <= 0xbf) {
            if (bytes[0] == 0xe0) {
                return bytes[1] >= 0xa0;
            }

            if (bytes[0] == 0xed) {
                return bytes[1] <= 0x9f;
            }

            return true;
        }

        return false;
    }

    public static boolean isUTF8ValidFourBytes(byte... bytes) {
        assert bytes.length == 4;

        if (bytes[3] < 0x80 || bytes[3] > 0xbf) {
            return false;
        }

        if (bytes[2] < 0x80 || bytes[2] > 0xbf) {
            return false;
        }

        if (bytes[0] < 0xf0 || bytes[0] > 0xf4) {
            return false;
        }

        if (bytes[1] >= 0x80 || bytes[2] <= 0xbf) {
            if (bytes[0] == 0xf0) {
                return bytes[1] >= 0x90;
            }

            if (bytes[0] == 0xf4) {
                return bytes[1] <= 0x8f;
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
