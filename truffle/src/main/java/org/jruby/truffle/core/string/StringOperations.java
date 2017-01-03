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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.language.RubyGuards;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class StringOperations {

    /** Creates a String from the ByteList, with unknown CR */
    public static DynamicObject createString(RubyContext context, ByteList bytes) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), RopeOperations.ropeFromByteList(bytes, CodeRange.CR_UNKNOWN));
    }

    public static DynamicObject createString(RubyContext context, Rope rope) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), rope);
    }

    public static String getString(DynamicObject string) {
        return RopeOperations.decodeRope(StringOperations.rope(string));
    }

    public static int clampExclusiveIndex(DynamicObject string, int index) {
        assert RubyGuards.isRubyString(string);

        // TODO (nirvdrum 21-Jan-16): Verify this is supposed to be the byteLength and not the characterLength.
        return ArrayOperations.clampExclusiveIndex(StringOperations.rope(string).byteLength(), index);
    }

    @TruffleBoundary
    public static Rope encodeRope(CharSequence value, Encoding encoding, CodeRange codeRange) {
        // Taken from org.jruby.RubyString#encodeByteList.

        Charset charset = encoding.getCharset();

        // if null charset, fall back on Java default charset
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        final ByteBuffer buffer = charset.encode(CharBuffer.wrap(value));
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);

        return RopeOperations.create(bytes, encoding, codeRange);
    }

    public static Rope encodeRope(CharSequence value, Encoding encoding) {
        return encodeRope(value, encoding, CodeRange.CR_UNKNOWN);
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

    /**
     * Create a byte[] from a CharSequence assuming a raw/ISO-8859-1 encoding
     *
     * @param s the CharSequence to convert
     * @return a byte[]
     */
    @TruffleBoundary
    public static byte[] plain(CharSequence s) {
        // Taken from org.jruby.util.ByteList.plain

        if (s instanceof String) {
            return StandardCharsets.ISO_8859_1.encode(CharBuffer.wrap(s)).array();
        }

        // Not a String...get it the slow way
        byte[] bytes = new byte[s.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) s.charAt(i);
        }
        return bytes;
    }
}
