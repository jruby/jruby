/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transposed from org.jruby.RubyString,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
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
 * Some of the code in this class is transliterated from C++ code in Rubinius.
 * 
 * Copyright (c) 2007-2014, Evan Phoenix and contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Rubinius nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.core.StringGuards;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.StringNodesFactory;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyRange;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.StringSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rubinius primitives associated with the Ruby {@code String} class.
 */
public abstract class StringPrimitiveNodes {

    @RubiniusPrimitive(name = "character_ascii_p")
    public static abstract class CharacterAsciiPrimitiveNode extends RubiniusPrimitiveNode {

        public CharacterAsciiPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isCharacterAscii(RubyBasicObject character) {
            final ByteList bytes = StringNodes.getByteList(character);
            final int codepoint = StringSupport.preciseCodePoint(
                    bytes.getEncoding(),
                    bytes.getUnsafeBytes(),
                    bytes.getBegin(),
                    bytes.getBegin() + bytes.getRealSize());

            final boolean found = codepoint != -1;

            return found && Encoding.isAscii(codepoint);
        }
    }

    @RubiniusPrimitive(name = "string_awk_split")
    public static abstract class StringAwkSplitPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private TaintResultNode taintResultNode;

        public StringAwkSplitPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject stringAwkSplit(RubyBasicObject string, int lim) {
            final List<RubyBasicObject> ret = new ArrayList<>();
            final ByteList value = StringNodes.getByteList(string);
            final boolean limit = lim > 0;
            int i = lim > 0 ? 1 : 0;

            byte[]bytes = value.getUnsafeBytes();
            int p = value.getBegin();
            int ptr = p;
            int len = value.getRealSize();
            int end = p + len;
            Encoding enc = value.getEncoding();
            boolean skip = true;

            int e = 0, b = 0;
            final boolean singlebyte = StringSupport.isSingleByteOptimizable(StringNodes.getCodeRangeable(string), enc);
            while (p < end) {
                final int c;
                if (singlebyte) {
                    c = bytes[p++] & 0xff;
                } else {
                    try {
                        c = StringSupport.codePoint(getContext().getRuntime(), enc, bytes, p, end);
                    } catch (org.jruby.exceptions.RaiseException ex) {
                        throw new RaiseException(getContext().toTruffle(ex.getException(), this));
                    }

                    p += StringSupport.length(enc, bytes, p, end);
                }

                if (skip) {
                    if (enc.isSpace(c)) {
                        b = p - ptr;
                    } else {
                        e = p - ptr;
                        skip = false;
                        if (limit && lim <= i) break;
                    }
                } else {
                    if (enc.isSpace(c)) {
                        ret.add(makeString(string, b, e - b));
                        skip = true;
                        b = p - ptr;
                        if (limit) i++;
                    } else {
                        e = p - ptr;
                    }
                }
            }

            if (len > 0 && (limit || len > b || lim < 0)) ret.add(makeString(string, b, len - b));

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), ret.toArray());
        }

        private RubyBasicObject makeString(RubyBasicObject source, int index, int length) {
            assert RubyGuards.isRubyString(source);

            final ByteList bytes = new ByteList(StringNodes.getByteList(source), index, length);
            bytes.setEncoding(StringNodes.getByteList(source).getEncoding());

            final RubyBasicObject ret = StringNodes.createString(source.getLogicalClass(), bytes);
            taintResultNode.maybeTaint(source, ret);

            return ret;
        }
    }

    @RubiniusPrimitive(name = "string_byte_substring")
    public static abstract class StringByteSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private TaintResultNode taintResultNode;

        public StringByteSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, int index, NotProvided length) {
            final Object subString = stringByteSubstring(string, index, 1);

            if (subString == nil()) {
                return subString;
            }

            if (StringNodes.getByteList((RubyBasicObject) subString).length() == 0) {
                return nil();
            }

            return subString;
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, int index, int length) {
            final ByteList bytes = StringNodes.getByteList(string);

            if (length < 0) {
                return nil();
            }

            final int normalizedIndex = StringNodes.normalizeIndex(string, index);

            if (normalizedIndex < 0 || normalizedIndex > bytes.length()) {
                return nil();
            }

            if (normalizedIndex + length > bytes.length()) {
                length = bytes.length() - normalizedIndex;
            }

            final RubyBasicObject result = StringNodes.createString(string.getLogicalClass(), new ByteList(bytes, normalizedIndex, length));

            return taintResultNode.maybeTaint(string, result);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, int index, double length) {
            return stringByteSubstring(string, index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, double index, NotProvided length) {
            return stringByteSubstring(string, (int) index, 1);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, long index, int length) {
            return stringByteSubstring(string, index, (long) length);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, int index, long length) {
            return stringByteSubstring(string, (long) index, length);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, long index, long length) {
            if (index > Integer.MAX_VALUE || index < Integer.MIN_VALUE) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("index out of int range", this));
            }
            if (length > Integer.MAX_VALUE || length < Integer.MIN_VALUE) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("length out of int range", this));
            }
            return stringByteSubstring(string, (int) index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, double index, double length) {
            return stringByteSubstring(string, (int) index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, double index, int length) {
            return stringByteSubstring(string, (int) index, length);
        }

        @Specialization
        public Object stringByteSubstring(RubyBasicObject string, RubyRange range, NotProvided length) {
            return null;
        }

        @Specialization(guards = "!isRubyRange(indexOrRange)")
        public Object stringByteSubstring(RubyBasicObject string, Object indexOrRange, Object length) {
            return null;
        }

    }

    @RubiniusPrimitive(name = "string_check_null_safe", needsSelf = false)
    public static abstract class StringCheckNullSafePrimitiveNode extends RubiniusPrimitiveNode {

        private final ConditionProfile nullByteProfile = ConditionProfile.createBinaryProfile();

        public StringCheckNullSafePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject stringCheckNullSafe(RubyBasicObject string) {
            final ByteList byteList = StringNodes.getByteList(string);

            for (int i = 0; i < byteList.length(); i++) {
                if (nullByteProfile.profile(byteList.get(i) == 0)) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().argumentError("string contains NULL byte", this));
                }
            }

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_chr_at")
    public static abstract class StringChrAtPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private StringByteSubstringPrimitiveNode stringByteSubstringNode;

        public StringChrAtPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object stringChrAt(RubyBasicObject string, int byteIndex) {
            // Taken from Rubinius's Character::create_from.

            final ByteList bytes = StringNodes.getByteList(string);

            if (byteIndex < 0 || byteIndex >= bytes.getRealSize()) {
                return nil();
            }

            final int p = bytes.getBegin() + byteIndex;
            final int end = bytes.getBegin() + bytes.getRealSize();
            final int c = StringSupport.preciseLength(bytes.getEncoding(), bytes.getUnsafeBytes(), p, end);

            if (! StringSupport.MBCLEN_CHARFOUND_P(c)) {
                return nil();
            }

            final int n = StringSupport.MBCLEN_CHARFOUND_LEN(c);
            if (n + byteIndex > end) {
                return nil();
            }

            if (stringByteSubstringNode == null) {
                CompilerDirectives.transferToInterpreter();

                stringByteSubstringNode = insert(
                        StringPrimitiveNodesFactory.StringByteSubstringPrimitiveNodeFactory.create(
                                getContext(),
                                getSourceSection(),
                                new RubyNode[]{})
                );
            }

            return stringByteSubstringNode.stringByteSubstring(string, byteIndex, n);
        }

    }

    @RubiniusPrimitive(name = "string_compare_substring")
    public static abstract class StringCompareSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        private final ConditionProfile startTooLargeProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile startTooSmallProfile = ConditionProfile.createBinaryProfile();

        @Child private StringNodes.SizeNode sizeNode;

        public StringCompareSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] { null });
        }

        @Specialization(guards = "isRubyString(other)")
        public int stringCompareSubstring(VirtualFrame frame, RubyBasicObject string, RubyBasicObject other, int start, int size) {
            // Transliterated from Rubinius C++.

            final int stringLength = sizeNode.executeInteger(frame, string);
            final int otherLength = sizeNode.executeInteger(frame, other);

            if (start < 0) {
                start += otherLength;
            }

            if (startTooLargeProfile.profile(start > otherLength)) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(
                                String.format("index %d out of string", start),
                                this
                        ));
            }

            if (startTooSmallProfile.profile(start < 0)) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(
                                String.format("index %d out of string", start),
                                this
                        ));
            }

            if (start + size > otherLength) {
                size = otherLength - start;
            }

            if (size > stringLength) {
                size = stringLength;
            }

            final ByteList bytes = StringNodes.getByteList(string);
            final ByteList otherBytes = StringNodes.getByteList(other);

            return ByteList.memcmp(bytes.getUnsafeBytes(), bytes.getBegin(), size,
                    otherBytes.getUnsafeBytes(), otherBytes.getBegin() + start, size);
        }

    }

    @RubiniusPrimitive(name = "string_equal", needsSelf = true)
    public static abstract class StringEqualPrimitiveNode extends RubiniusPrimitiveNode {

        private final ConditionProfile incompatibleEncodingProfile = ConditionProfile.createBinaryProfile();

        public StringEqualPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(other)")
        public boolean stringEqual(RubyBasicObject string, RubyBasicObject other) {
            final ByteList a = StringNodes.getByteList(string);
            final ByteList b = StringNodes.getByteList(other);

            if (incompatibleEncodingProfile.profile((a.getEncoding() != b.getEncoding()) &&
                    (org.jruby.RubyEncoding.areCompatible(StringNodes.getCodeRangeable(string), StringNodes.getCodeRangeable(other)) == null))) {
                return false;
            }

            return a.equal(b);
        }

    }

    @RubiniusPrimitive(name = "string_find_character")
    @ImportStatic(StringGuards.class)
    public static abstract class StringFindCharacterNode extends RubiniusPrimitiveNode {

        @Child private TaintResultNode taintResultNode;

        public StringFindCharacterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSingleByte(string)")
        public Object stringFindCharacterSingleByte(RubyBasicObject string, int offset) {
            // Taken from Rubinius's String::find_character.

            if (offset < 0) {
                return nil();
            }

            final ByteList byteList = StringNodes.getByteList(string);
            if (offset >= byteList.getRealSize()) {
                return nil();
            }

            final RubyBasicObject ret = StringNodes.createString(string.getLogicalClass(), new ByteList(byteList, offset, 1));

            return propagate(string, ret);
        }

        @Specialization(guards = "!isSingleByte(string)")
        public Object stringFindCharacter(RubyBasicObject string, int offset) {
            // Taken from Rubinius's String::find_character.

            if (offset < 0) {
                return nil();
            }

            final ByteList byteList = StringNodes.getByteList(string);
            if (offset >= byteList.getRealSize()) {
                return nil();
            }

            final ByteList bytes = byteList;
            final Encoding enc = bytes.getEncoding();
            final int clen = StringSupport.preciseLength(enc, bytes.getUnsafeBytes(), bytes.begin(), bytes.begin() + bytes.realSize());

            final RubyBasicObject ret;
            if (StringSupport.MBCLEN_CHARFOUND_P(clen)) {
                ret = StringNodes.createString(string.getLogicalClass(), new ByteList(byteList, offset, clen));
            } else {
                ret = StringNodes.createString(string.getLogicalClass(), new ByteList(byteList, offset, 1));
            }

            return propagate(string, ret);
        }

        private Object propagate(RubyBasicObject string, RubyBasicObject ret) {
            StringNodes.getByteList(ret).setEncoding(StringNodes.getByteList(string).getEncoding());
            StringNodes.setCodeRange(ret, StringNodes.getCodeRange(string));
            return maybeTaint(string, ret);
        }

        private Object maybeTaint(RubyBasicObject source, RubyBasicObject value) {
            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }
            return taintResultNode.maybeTaint(source, value);
        }

    }

    @RubiniusPrimitive(name = "string_from_codepoint", needsSelf = false)
    public static abstract class StringFromCodepointPrimitiveNode extends RubiniusPrimitiveNode {

        public StringFromCodepointPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSimple(code, encoding)")
        public RubyBasicObject stringFromCodepointSimple(int code, RubyEncoding encoding) {
            return StringNodes.createString(
                    getContext().getCoreLibrary().getStringClass(),
                    new ByteList(new byte[]{(byte) code}, encoding.getEncoding()));
        }

        @TruffleBoundary
        @Specialization(guards = "!isSimple(code, encoding)")
        public RubyBasicObject stringFromCodepoint(int code, RubyEncoding encoding) {
            final int length;

            try {
                length = encoding.getEncoding().codeToMbcLength(code);
            } catch (EncodingException e) {
                throw new RaiseException(getContext().getCoreLibrary().rangeError(code, encoding, this));
            }

            if (length <= 0) {
                throw new RaiseException(getContext().getCoreLibrary().rangeError(code, encoding, this));
            }

            final byte[] bytes = new byte[length];

            try {
                encoding.getEncoding().codeToMbc(code, bytes, 0);
            } catch (EncodingException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(code, encoding, this));
            }

            return StringNodes.createString(
                    getContext().getCoreLibrary().getStringClass(),
                    new ByteList(bytes, encoding.getEncoding()));
        }

        @Specialization
        public RubyBasicObject stringFromCodepointSimple(long code, RubyEncoding encoding) {
            if (code < Integer.MIN_VALUE || code > Integer.MAX_VALUE) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }

            return stringFromCodepointSimple((int) code, encoding);
        }

        protected boolean isSimple(int code, RubyEncoding encoding) {
            return encoding.getEncoding() == ASCIIEncoding.INSTANCE && code >= 0x00 && code <= 0xFF;
        }

    }

    @RubiniusPrimitive(name = "string_to_f", needsSelf = false)
    public static abstract class StringToFPrimitiveNode extends RubiniusPrimitiveNode {

        public StringToFPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object stringToF(RubyBasicObject string) {
            try {
                return Double.parseDouble(string.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    @RubiniusPrimitive(name = "string_index", lowerFixnumParameters = 1)
    public static abstract class StringIndexPrimitiveNode extends RubiniusPrimitiveNode {

        @Child StringByteCharacterIndexNode byteIndexToCharIndexNode;

        public StringIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {

            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringIndex(VirtualFrame frame, RubyBasicObject string, RubyBasicObject pattern, int start) {
            if (byteIndexToCharIndexNode == null) {
                CompilerDirectives.transferToInterpreter();
                byteIndexToCharIndexNode = insert(StringPrimitiveNodesFactory.StringByteCharacterIndexNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
            }

            // Rubinius will pass in a byte index for the `start` value, but StringSupport.index requires a character index.
            final int charIndex = byteIndexToCharIndexNode.executeStringBytCharacterIndex(frame, string, start, 0);

            final int index = StringSupport.index(StringNodes.getCodeRangeable(string),
                    StringNodes.getCodeRangeable(pattern),
                    charIndex, StringNodes.getByteList(string).getEncoding());

            if (index == -1) {
                return nil();
            }

            return index;
        }

    }

    @RubiniusPrimitive(name = "string_character_byte_index", needsSelf = false, lowerFixnumParameters = {1, 2})
    @ImportStatic(StringGuards.class)
    public static abstract class CharacterByteIndexNode extends RubiniusPrimitiveNode {

        public CharacterByteIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int stringCharacterByteIndex(RubyBasicObject string, int index, int start) {
            return start + index;
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public int stringCharacterByteIndexMultiByteEncoding(RubyBasicObject string, int index, int start) {
            final ByteList bytes = StringNodes.getByteList(string);

            return StringSupport.nth(bytes.getEncoding(), bytes.getUnsafeBytes(), bytes.getBegin() + start,
                    bytes.getBegin() + bytes.getRealSize(), index) - bytes.begin();
        }
    }

    @RubiniusPrimitive(name = "string_byte_character_index", needsSelf = false)
    @ImportStatic(StringGuards.class)
    public static abstract class StringByteCharacterIndexNode extends RubiniusPrimitiveNode {

        public StringByteCharacterIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract int executeStringBytCharacterIndex(VirtualFrame frame, RubyBasicObject string, int index, int start);

        @Specialization(guards = "isSingleByteOptimizableOrAsciiOnly(string)")
        public int stringByteCharacterIndexSingleByte(RubyBasicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index;
        }

        @Specialization(guards = { "!isSingleByteOptimizableOrAsciiOnly(string)", "isFixedWidthEncoding(string)", "!isValidUtf8(string)" })
        public int stringByteCharacterIndexFixedWidth(RubyBasicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index / StringNodes.getByteList(string).getEncoding().minLength();
        }

        @Specialization(guards = { "!isSingleByteOptimizableOrAsciiOnly(string)", "!isFixedWidthEncoding(string)", "isValidUtf8(string)" })
        public int stringByteCharacterIndexValidUtf8(RubyBasicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.

            // TODO (nirvdrum 02-Apr-15) There's a way to optimize this for UTF-8, but porting all that code isn't necessary at the moment.
            return stringByteCharacterIndex(string, index, start);
        }

        @TruffleBoundary
        @Specialization(guards = { "!isSingleByteOptimizableOrAsciiOnly(string)", "!isFixedWidthEncoding(string)", "!isValidUtf8(string)" })
        public int stringByteCharacterIndex(RubyBasicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index and Encoding::find_byte_character_index.

            final ByteList bytes = StringNodes.getByteList(string);
            final Encoding encoding = bytes.getEncoding();
            int p = bytes.begin() + start;
            final int end = bytes.begin() + bytes.realSize();
            int charIndex = 0;

            while (p < end && index > 0) {
                final int charLen = StringSupport.length(encoding, bytes.getUnsafeBytes(), p, end);
                p += charLen;
                index -= charLen;
                charIndex++;
            }

            return charIndex;
        }
    }

    @RubiniusPrimitive(name = "string_character_index", needsSelf = false, lowerFixnumParameters = 2)
    public static abstract class StringCharacterIndexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringCharacterIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(pattern)")
        public Object stringCharacterIndex(RubyBasicObject string, RubyBasicObject pattern, int offset) {
            if (offset < 0) {
                return nil();
            }

            final int total = StringNodes.getByteList(string).length();
            int p = StringNodes.getByteList(string).getBegin();
            final int e = p + total;
            int pp = StringNodes.getByteList(pattern).getBegin();
            final int pe = pp + StringNodes.getByteList(pattern).length();
            int s;
            int ss;

            final byte[] stringBytes = StringNodes.getByteList(string).getUnsafeBytes();
            final byte[] patternBytes = StringNodes.getByteList(pattern).getUnsafeBytes();

            if (StringSupport.isSingleByteOptimizable(StringNodes.getCodeRangeable(string), StringNodes.getByteList(string).getEncoding())) {
                for(s = p += offset, ss = pp; p < e; s = ++p) {
                    if (stringBytes[p] != patternBytes[pp]) continue;

                    while (p < e && pp < pe && stringBytes[p] == patternBytes[pp]) {
                        p++;
                        pp++;
                    }

                    if (pp < pe) {
                        p = s;
                        pp = ss;
                    } else {
                        return s;
                    }
                }

                return nil();
            }

            final Encoding enc = StringNodes.getByteList(string).getEncoding();
            int index = 0;
            int c;

            while(p < e && index < offset) {
                c = StringSupport.preciseLength(enc, stringBytes, p, e);

                if (StringSupport.MBCLEN_CHARFOUND_P(c)) {
                    p += c;
                    index++;
                } else {
                    return nil();
                }
            }

            for(s = p, ss = pp; p < e; s = p += c, ++index) {
                c = StringSupport.preciseLength(enc, stringBytes, p, e);
                if ( !StringSupport.MBCLEN_CHARFOUND_P(c)) return nil();

                if (stringBytes[p] != patternBytes[pp]) continue;

                while (p < e && pp < pe) {
                    boolean breakOut = false;

                    for (int pc = p + c; p < e && p < pc && pp < pe; ) {
                        if (stringBytes[p] == patternBytes[pp]) {
                            ++p;
                            ++pp;
                        } else {
                            breakOut = true;
                            break;
                        }
                    }

                    if (breakOut) {
                        break;
                    }

                    c = StringSupport.preciseLength(enc, stringBytes, p, e);
                    if (! StringSupport.MBCLEN_CHARFOUND_P(c)) break;
                }

                if (pp < pe) {
                    p = s;
                    pp = ss;
                } else {
                    return index;
                }
            }

            return nil();
        }

    }

    @RubiniusPrimitive(name = "string_byte_index", needsSelf = false, lowerFixnumParameters = {1, 2})
    public static abstract class StringByteIndexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringByteIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object stringByteIndex(RubyBasicObject string, int index, int start) {
            // Taken from Rubinius's String::byte_index.

            final ByteList bytes = StringNodes.getByteList(string);

            final Encoding enc = bytes.getEncoding();
            int p = bytes.getBegin();
            final int e = p + bytes.getRealSize();

            int i, k = index;

            if (k < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("character index is negative", this));
            }

            for (i = 0; i < k && p < e; i++) {
                final int c = StringSupport.preciseLength(enc, bytes.getUnsafeBytes(), p, e);

                // If it's an invalid byte, just treat it as a single byte
                if(! StringSupport.MBCLEN_CHARFOUND_P(c)) {
                    ++p;
                } else {
                    p += StringSupport.MBCLEN_CHARFOUND_LEN(c);
                }
            }

            if (i < k) {
                return nil();
            } else {
                return p;
            }
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringByteIndex(RubyBasicObject string, RubyBasicObject pattern, int offset) {
            // Taken from Rubinius's String::byte_index.

            final int match_size = StringNodes.getByteList(pattern).length();

            if (offset < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative start given", this));
            }

            if (match_size == 0) return offset;

            if (StringNodes.scanForCodeRange(string) == StringSupport.CR_BROKEN) {
                return nil();
            }

            final Encoding encoding = StringNodes.checkEncoding(string, StringNodes.getCodeRangeable(pattern), this);
            int p = StringNodes.getByteList(string).getBegin();
            final int e = p + StringNodes.getByteList(string).getRealSize();
            int pp = StringNodes.getByteList(pattern).getBegin();
            final int pe = pp + StringNodes.getByteList(pattern).getRealSize();
            int s;
            int ss;

            final byte[] stringBytes = StringNodes.getByteList(string).getUnsafeBytes();
            final byte[] patternBytes = StringNodes.getByteList(pattern).getUnsafeBytes();

            for(s = p, ss = pp; p < e; s = ++p) {
                if (stringBytes[p] != patternBytes[pp]) continue;

                while (p < e && pp < pe && stringBytes[p] == patternBytes[pp]) {
                    p++;
                    pp++;
                }

                if (pp < pe) {
                    p = s;
                    pp = ss;
                } else {
                    final int c = StringSupport.preciseLength(encoding, stringBytes, s, e);

                    if (StringSupport.MBCLEN_CHARFOUND_P(c)) {
                        return s;
                    } else {
                        return nil();
                    }
                }
            }

            return nil();
        }
    }

    @RubiniusPrimitive(name = "string_previous_byte_index")
    public static abstract class StringPreviousByteIndexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringPreviousByteIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object stringPreviousByteIndex(RubyBasicObject string, int index) {
            // Port of Rubinius's String::previous_byte_index.

            if (index < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative index given", this));
            }

            final ByteList bytes = StringNodes.getByteList(string);
            final int p = bytes.getBegin();
            final int end = p + bytes.getRealSize();

            final int b = bytes.getEncoding().prevCharHead(bytes.getUnsafeBytes(), p, p + index, end);

            if (b == -1) {
                return nil();
            }

            return b - p;
        }

    }

    @RubiniusPrimitive(name = "string_copy_from", needsSelf = false, lowerFixnumParameters = {2, 3, 4})
    public static abstract class StringCopyFromPrimitiveNode extends RubiniusPrimitiveNode {

        public StringCopyFromPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(other)")
        public RubyBasicObject stringCopyFrom(RubyBasicObject string, RubyBasicObject other, int start, int size, int dest) {
            // Taken from Rubinius's String::copy_from.

            int src = start;
            int dst = dest;
            int cnt = size;

            final ByteList otherBytes = StringNodes.getByteList(other);
            int osz = otherBytes.length();
            if(src >= osz) return string;
            if(cnt < 0) return string;
            if(src < 0) src = 0;
            if(cnt > osz - src) cnt = osz - src;

            // This bounds checks on the total capacity rather than the virtual
            // size() of the String. This allows for string adjustment within
            // the capacity without having to change the virtual size first.
            final ByteList stringBytes = StringNodes.getByteList(string);
            int sz = stringBytes.unsafeBytes().length - stringBytes.begin();
            if(dst >= sz) return string;
            if(dst < 0) dst = 0;
            if(cnt > sz - dst) cnt = sz - dst;

            System.arraycopy(otherBytes.unsafeBytes(), otherBytes.begin() + src, stringBytes.getUnsafeBytes(), stringBytes.begin() + dest, cnt);

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_resize_capacity", needsSelf = false, lowerFixnumParameters = 1)
    public static abstract class StringResizeCapacityPrimitiveNode extends RubiniusPrimitiveNode {

        public StringResizeCapacityPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject stringResizeCapacity(RubyBasicObject string, int capacity) {
            StringNodes.getByteList(string).ensure(capacity);
            return string;
        }

    }

    @RubiniusPrimitive(name = "string_rindex", lowerFixnumParameters = 1)
    public static abstract class StringRindexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringRindexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringRindex(RubyBasicObject string, RubyBasicObject pattern, int start) {
            // Taken from Rubinius's String::rindex.

            int pos = start;

            if (pos < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative start given", this));
            }

            final ByteList buf = StringNodes.getByteList(string);
            final int total = buf.getRealSize();
            final int matchSize = StringNodes.getByteList(pattern).getRealSize();

            if (pos >= total) {
                pos = total - 1;
            }

            switch(matchSize) {
                case 0: {
                    return start;
                }

                case 1: {
                    final int matcher = StringNodes.getByteList(pattern).get(0);

                    while (pos >= 0) {
                        if (buf.get(pos) == matcher) {
                            return pos;
                        }

                        pos--;
                    }

                    return nil();
                }

                default: {
                    if (total - pos < matchSize) {
                        pos = total - matchSize;
                    }

                    int cur = pos;

                    while (cur >= 0) {
                        if (ByteList.memcmp(StringNodes.getByteList(string).getUnsafeBytes(), cur, StringNodes.getByteList(pattern).getUnsafeBytes(), 0, matchSize) == 0) {
                            return cur;
                        }

                        cur--;
                    }
                }
            }

            return nil();
        }

    }

    @RubiniusPrimitive(name = "string_pattern", lowerFixnumParameters = {0, 1})
    public static abstract class StringPatternPrimitiveNode extends RubiniusPrimitiveNode {

        public StringPatternPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }


        @Specialization(guards = "value == 0")
        public RubyBasicObject stringPatternZero(RubyClass stringClass, int size, int value) {
            return StringNodes.createString(stringClass, new ByteList(new byte[size]));
        }

        @Specialization(guards = "value != 0")
        public RubyBasicObject stringPattern(RubyClass stringClass, int size, int value) {
            final byte[] bytes = new byte[size];
            Arrays.fill(bytes, (byte) value);
            return StringNodes.createString(stringClass, new ByteList(bytes));
        }

        @Specialization(guards = "isRubyString(string)")
        public RubyBasicObject stringPattern(RubyClass stringClass, int size, RubyBasicObject string) {
            final byte[] bytes = new byte[size];
            final ByteList byteList = StringNodes.getByteList(string);

            if (byteList.length() > 0) {
                for (int n = 0; n < size; n += byteList.length()) {
                    System.arraycopy(byteList.unsafeBytes(), byteList.begin(), bytes, n, Math.min(byteList.length(), size - n));
                }
            }
            
            return StringNodes.createString(stringClass, new ByteList(bytes));
        }

    }

    @RubiniusPrimitive(name = "string_to_inum")
    public static abstract class StringToInumPrimitiveNode extends RubiniusPrimitiveNode {

        public StringToInumPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object stringToInum(RubyBasicObject string, int fixBase, boolean strict) {
            try {
                final org.jruby.RubyInteger result = ConvertBytes.byteListToInum19(getContext().getRuntime(),
                        StringNodes.getByteList(string),
                        fixBase,
                        strict);

                return getContext().toTruffle(result);
            } catch (org.jruby.exceptions.RaiseException e) {
                throw new RaiseException(getContext().toTruffle(e.getException(), this));
            }
        }

    }

    @RubiniusPrimitive(name = "string_byte_append")
    public static abstract class StringByteAppendPrimitiveNode extends RubiniusPrimitiveNode {

        public StringByteAppendPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(other)")
        public RubyBasicObject stringByteAppend(RubyBasicObject string, RubyBasicObject other) {
            StringNodes.getByteList(string).append(StringNodes.getByteList(other));
            return string;
        }

    }

    @RubiniusPrimitive(name = "string_substring")
    @ImportStatic(StringGuards.class)
    public static abstract class StringSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private TaintResultNode taintResultNode;

        public StringSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object execute(VirtualFrame frame, RubyBasicObject string, int beg, int len);

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public Object stringSubstringSingleByteOptimizable(RubyBasicObject string, int beg, int len) {
            // Taken from org.jruby.RubyString#substr19.

            if (len < 0) {
                return nil();
            }

            final int length = StringNodes.getByteList(string).getRealSize();
            if (length == 0) {
                len = 0;
            }

            if (beg > length) {
                return nil();
            }

            if (beg < 0) {
                beg += length;

                if (beg < 0) {
                    return nil();
                }
            }

            if ((beg + len) > length) {
                len = length - beg;
            }

            if (len <= 0) {
                len = 0;
                beg = 0;
            }

            return makeSubstring(string, beg, len);
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public Object stringSubstring(RubyBasicObject string, int beg, int len) {
            // Taken from org.jruby.RubyString#substr19 & org.jruby.RubyString#multibyteSubstr19.

            if (len < 0) {
                return nil();
            }

            final int length = StringNodes.getByteList(string).getRealSize();
            if (length == 0) {
                len = 0;
            }

            if ((beg + len) > length) {
                len = length - beg;
            }

            final ByteList value = StringNodes.getByteList(string);
            final Encoding enc = value.getEncoding();
            int p;
            int s = value.getBegin();
            int end = s + length;
            byte[]bytes = value.getUnsafeBytes();

            if (beg < 0) {
                if (len > -beg) len = -beg;
                if (-beg * enc.maxLength() < length >>> 3) {
                    beg = -beg;
                    int e = end;
                    while (beg-- > len && (e = enc.prevCharHead(bytes, s, e, e)) != -1) {} // nothing
                    p = e;
                    if (p == -1) {
                        return nil();
                    }
                    while (len-- > 0 && (p = enc.prevCharHead(bytes, s, p, e)) != -1) {} // nothing
                    if (p == -1) {
                        return nil();
                    }
                    return makeSubstring(string, p - s, e - p);
                } else {
                    beg += StringSupport.strLengthFromRubyString(StringNodes.getCodeRangeable(string), enc);
                    if (beg < 0) {
                        return nil();
                    }
                }
            } else if (beg > 0 && beg > StringSupport.strLengthFromRubyString(StringNodes.getCodeRangeable(string), enc)) {
                return nil();
            }
            if (len == 0) {
                p = 0;
            } else if (StringNodes.isCodeRangeValid(string) && enc instanceof UTF8Encoding) {
                p = StringSupport.utf8Nth(bytes, s, end, beg);
                len = StringSupport.utf8Offset(bytes, p, end, len);
            } else if (enc.isFixedWidth()) {
                int w = enc.maxLength();
                p = s + beg * w;
                if (p > end) {
                    p = end;
                    len = 0;
                } else if (len * w > end - p) {
                    len = end - p;
                } else {
                    len *= w;
                }
            } else if ((p = StringSupport.nth(enc, bytes, s, end, beg)) == end) {
                len = 0;
            } else {
                len = StringSupport.offset(enc, bytes, p, end, len);
            }
            return makeSubstring(string, p - s, len);
        }

        private RubyBasicObject makeSubstring(RubyBasicObject string, int beg, int len) {
            assert RubyGuards.isRubyString(string);

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            final RubyBasicObject ret = StringNodes.createString(string.getLogicalClass(), new ByteList(StringNodes.getByteList(string), beg, len));
            StringNodes.getByteList(ret).setEncoding(StringNodes.getByteList(string).getEncoding());
            taintResultNode.maybeTaint(string, ret);

            return ret;
        }

    }

    @RubiniusPrimitive(name = "string_from_bytearray", needsSelf = false, lowerFixnumParameters = {1, 2})
    public static abstract class StringFromByteArrayPrimitiveNode extends RubiniusPrimitiveNode {

        public StringFromByteArrayPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubiniusByteArray(bytes)")
        public RubyBasicObject stringFromByteArray(RubyBasicObject bytes, int start, int count) {
            // Data is copied here - can we do something COW?
            final ByteList byteList = ByteArrayNodes.getBytes(bytes);
            return createString(new ByteList(byteList, start, count));
        }

    }

}
