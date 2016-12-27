/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007-2010 JRuby Community
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 */
package org.jruby.truffle.parser;

import org.jcodings.Encoding;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringSupport;
import org.jruby.truffle.parser.lexer.RubyLexer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.jruby.truffle.core.rope.CodeRange.CR_UNKNOWN;

public class ParserByteList {

    private final byte[] bytes;
    private final int start;
    private final int length;
    private final Encoding encoding;

    public ParserByteList(byte[] bytes) {
        this(bytes, ASCIIEncoding.INSTANCE);
    }

    public ParserByteList(byte[] bytes, Encoding encoding) {
        this(bytes, 0, bytes.length, encoding);
    }

    public ParserByteList(byte[] bytes, int start, int length, Encoding encoding) {
        this.bytes = bytes;
        this.start = start;
        this.length = length;
        this.encoding = encoding;
    }

    public int getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public ParserByteList withEncoding(Encoding encoding) {
        return new ParserByteList(bytes, start, length, encoding);
    }

    public ParserByteList makeShared(int sharedStart, int sharedLength) {
        return new ParserByteList(bytes, start + sharedStart, sharedLength, encoding);
    }

    public int caseInsensitiveCmp(ParserByteList other) {
        if (other == this) return 0;

        final int size = length;
        final int len =  Math.min(size, other.length);
        final int other_begin = other.start;
        final byte[] other_bytes = other.bytes;

        for (int offset = -1; ++offset < len;) {
            int myCharIgnoreCase = AsciiTables.ToLowerCaseTable[bytes[start + offset] & 0xff] & 0xff;
            int otherCharIgnoreCase = AsciiTables.ToLowerCaseTable[other_bytes[other_begin + offset] & 0xff] & 0xff;
            if (myCharIgnoreCase < otherCharIgnoreCase) {
                return -1;
            } else if (myCharIgnoreCase > otherCharIgnoreCase) {
                return 1;
            }
        }
        return size == other.length ? 0 : size == len ? -1 : 1;
    }

    public boolean equal(ParserByteList other) {
        if (other == this) return true;

        int first, last;
        if ((last = length) == other.length) {
            byte buf[] = bytes;
            byte otherBuf[] = other.bytes;
            // scanning from front and back simultaneously, meeting in
            // the middle. the object is to get a mismatch as quickly as
            // possible. alternatives might be: scan from the middle outward
            // (not great because it won't pick up common variations at the
            // ends until late) or sample odd bytes forward and even bytes
            // backward (I like this one, but it's more expensive for
            // strings that are equal; see sample_equals below).
            first = -1;
            while (--last > first && buf[start + last] == otherBuf[other.start + last] &&
                    ++first < last && buf[start + first] == otherBuf[other.start + first]) {
            }
            return first >= last;
        }
        return false;
    }

    public int charAt(int index) {
        return bytes[start + index];
    }

    @Override
    public String toString() {
        return StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(bytes, start, length)).toString();
    }

    public Rope toRope(CodeRange codeRange) {
        return RopeOperations.create(getBytes(), encoding, codeRange);
    }

    public Rope toRope() {
        return toRope(CR_UNKNOWN);
    }

    public byte[] getBytes() {
        return Arrays.copyOfRange(bytes, start, start + length);
    }

    public CodeRange codeRangeScan(Encoding encoding) {
        return StringSupport.codeRangeScan(encoding, bytes, start, length);
    }

    public int getStringLength(Encoding encoding) {
        return encoding.strLength(bytes, start, length);
    }

    public int getEncodingLength(Encoding encoding) {
        return encoding.length(bytes, start, length);
    }

    public int getStringLength() {
        return getStringLength(encoding);
    }

    public String toEncodedString(Encoding encoding) {
        return RubyLexer.createAsEncodedString(bytes, start, length, encoding);
    }

}
