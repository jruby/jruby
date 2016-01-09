/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
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
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.EncodingOperations;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.SubstringRope;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.StringSupport;
import static org.jruby.truffle.runtime.core.StringOperations.rope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rubinius primitives associated with the Ruby {@code String} class.
 */
public abstract class StringPrimitiveNodes {

    @RubiniusPrimitive(name = "character_ascii_p")
    @ImportStatic(StringGuards.class)
    public static abstract class CharacterAsciiPrimitiveNode extends RubiniusPrimitiveNode {

        public CharacterAsciiPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "is7Bit(character)")
        public boolean isCharacterAscii(DynamicObject character) {
            return ! rope(character).isEmpty();
        }

        @Specialization(guards = "!is7Bit(character)")
        public boolean isCharacterAsciiMultiByte(DynamicObject character) {
            final Rope rope = rope(character);
            final int codepoint = StringSupport.preciseCodePoint(
                    rope.getEncoding(),
                    rope.getBytes(),
                    0,
                    rope.byteLength());

            final boolean found = codepoint != -1;

            return found && Encoding.isAscii(codepoint);
        }
    }

    @RubiniusPrimitive(name = "character_printable_p")
    public static abstract class CharacterPrintablePrimitiveNode extends RubiniusPrimitiveNode {

        public CharacterPrintablePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean isCharacterPrintable(DynamicObject character) {
            final Rope rope = rope(character);
            final Encoding encoding = rope.getEncoding();

            final int codepoint = encoding.mbcToCode(rope.getBytes(), 0, rope.byteLength());

            return encoding.isPrint(codepoint);
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
        public DynamicObject stringAwkSplit(DynamicObject string, int lim) {
            final List<DynamicObject> ret = new ArrayList<>();
            final ByteList value = StringOperations.getByteListReadOnly(string);
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
            final boolean singlebyte = StringSupport.isSingleByteOptimizable(StringOperations.getCodeRangeable(string), enc);
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

            Object[] objects = ret.toArray();
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length);
        }

        private DynamicObject makeString(DynamicObject source, int index, int length) {
            assert RubyGuards.isRubyString(source);

            final ByteList bytes = new ByteList(StringOperations.getByteListReadOnly(source), index, length);
            bytes.setEncoding(Layouts.STRING.getRope(source).getEncoding());

            // TODO (nirvdrum 08-Jan-16) We should be able to work out the code range from the parent string in some cases.
            final DynamicObject ret = Layouts.STRING.createString(Layouts.CLASS.getInstanceFactory(Layouts.BASIC_OBJECT.getLogicalClass(source)), StringOperations.ropeFromByteList(bytes, StringSupport.CR_UNKNOWN), null);
            taintResultNode.maybeTaint(source, ret);

            return ret;
        }
    }

    @RubiniusPrimitive(name = "string_byte_substring")
    public static abstract class StringByteSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private TaintResultNode taintResultNode;
        @Child private AllocateObjectNode allocateObjectNode;
        @Child private StringNodes.SizeNode sizeNode;

        public StringByteSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, int index, NotProvided length) {
            final Object subString = stringByteSubstring(frame, string, index, 1);

            if (subString == nil()) {
                return subString;
            }

            if (StringOperations.getByteList((DynamicObject) subString).length() == 0) {
                return nil();
            }

            return subString;
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, int index, int length) {
            final ByteList bytes = StringOperations.getByteListReadOnly(string);

            if (length < 0) {
                return nil();
            }

            final int stringLength = getSizeNode().executeInteger(frame, string);
            final int normalizedIndex = StringOperations.normalizeIndex(stringLength, index);

            if (normalizedIndex < 0 || normalizedIndex > bytes.length()) {
                return nil();
            }

            if (normalizedIndex + length > bytes.length()) {
                length = bytes.length() - normalizedIndex;
            }

            final int codeRange = StringOperations.getCodeRange(string) == StringSupport.CR_7BIT ? StringSupport.CR_7BIT : StringSupport.CR_UNKNOWN;
            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(new ByteList(bytes, normalizedIndex, length), codeRange), null);

            return taintResultNode.maybeTaint(string, result);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, int index, long length) {
            return stringByteSubstring(frame, string, (long) index, length);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, int index, double length) {
            return stringByteSubstring(frame, string, index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(DynamicObject string, int index, DynamicObject length) {
            return null;
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, long index, NotProvided length) {
            return stringByteSubstring(frame, string, index, 1);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, long index, int length) {
            return stringByteSubstring(frame, string, index, (long) length);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, long index, long length) {
            if (index > Integer.MAX_VALUE || index < Integer.MIN_VALUE) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("index out of int range", this));
            }
            if (length > Integer.MAX_VALUE || length < Integer.MIN_VALUE) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("length out of int range", this));
            }
            return stringByteSubstring(frame, string, (int) index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame,DynamicObject string, long index, double length) {
            return stringByteSubstring(frame, string, index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(DynamicObject string, long index, DynamicObject length) {
            return null;
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, double index, NotProvided length) {
            return stringByteSubstring(frame, string, (int) index, 1);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, double index, int length) {
            return stringByteSubstring(frame, string, (int) index, length);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, double index, long length) {
            return stringByteSubstring(frame, string, (int) index, length);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, double index, double length) {
            return stringByteSubstring(frame, string, (int) index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(DynamicObject string, double index, DynamicObject length) {
            return null;
        }

        @Specialization(guards = "isRubyRange(range)")
        public Object stringByteSubstring(DynamicObject string, DynamicObject range, NotProvided length) {
            return null;
        }

        @Specialization(guards = "!isRubyRange(index)")
        public Object stringByteSubstring(DynamicObject string, DynamicObject index, Object length) {
            return null;
        }

        private StringNodes.SizeNode getSizeNode() {
            if (sizeNode == null) {
                CompilerDirectives.transferToInterpreter();
                sizeNode = insert(StringNodesFactory.SizeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null}));
            }

            return sizeNode;
        }
    }

    @RubiniusPrimitive(name = "string_check_null_safe", needsSelf = false)
    public static abstract class StringCheckNullSafePrimitiveNode extends RubiniusPrimitiveNode {

        public StringCheckNullSafePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject stringCheckNullSafe(DynamicObject string) {
            final ByteList byteList = StringOperations.getByteList(string);

            for (int i = 0; i < byteList.length(); i++) {
                if (byteList.get(i) == 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().argumentError("string contains NULL byte", this));
                }
            }

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_chr_at", lowerFixnumParameters = 0)
    public static abstract class StringChrAtPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private StringByteSubstringPrimitiveNode stringByteSubstringNode;

        public StringChrAtPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            stringByteSubstringNode = StringPrimitiveNodesFactory.StringByteSubstringPrimitiveNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {});
        }

        @Specialization(guards = "indexOutOfBounds(string, byteIndex)")
        public Object stringChrAtOutOfBounds(DynamicObject string, int byteIndex) {
            return false;
        }

        @Specialization(guards = "!indexOutOfBounds(string, byteIndex)")
        public Object stringChrAt(VirtualFrame frame, DynamicObject string, int byteIndex) {
            // Taken from Rubinius's Character::create_from.

            final Rope rope = rope(string);
            final int end = rope.byteLength();
            final int c = preciseLength(rope, byteIndex, end);

            if (! StringSupport.MBCLEN_CHARFOUND_P(c)) {
                return nil();
            }

            final int n = StringSupport.MBCLEN_CHARFOUND_LEN(c);
            if (n + byteIndex > end) {
                return nil();
            }

            return stringByteSubstringNode.stringByteSubstring(frame, string, byteIndex, n);
        }

        @TruffleBoundary
        private int preciseLength(final Rope rope, final int p, final int end) {
            return StringSupport.preciseLength(rope.getEncoding(), rope.getBytes(), p, end);
        }

        protected static boolean indexOutOfBounds(DynamicObject string, int byteIndex) {
            return ((byteIndex < 0) || (byteIndex >= rope(string).byteLength()));
        }

    }

    @RubiniusPrimitive(name = "string_compare_substring")
    public static abstract class StringCompareSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private StringNodes.SizeNode sizeNode;

        public StringCompareSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] { null });
        }

        @Specialization(guards = "isRubyString(other)")
        public int stringCompareSubstring(VirtualFrame frame, DynamicObject string, DynamicObject other, int start, int size) {
            // Transliterated from Rubinius C++.

            final int stringLength = sizeNode.executeInteger(frame, string);
            final int otherLength = sizeNode.executeInteger(frame, other);

            if (start < 0) {
                start += otherLength;
            }

            if (start > otherLength) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(
                                String.format("index %d out of string", start),
                                this
                        ));
            }

            if (start < 0) {
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

            final ByteList bytes = StringOperations.getByteListReadOnly(string);
            final ByteList otherBytes = StringOperations.getByteListReadOnly(other);

            return ByteList.memcmp(bytes.getUnsafeBytes(), bytes.getBegin(), size,
                    otherBytes.getUnsafeBytes(), otherBytes.getBegin() + start, size);
        }

    }

    @RubiniusPrimitive(name = "string_equal", needsSelf = true)
    public static abstract class StringEqualPrimitiveNode extends RubiniusPrimitiveNode {

        public StringEqualPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract boolean executeStringEqual(DynamicObject string, DynamicObject other);

        @Specialization(guards = "string == other")
        public boolean stringEqualsSameObject(DynamicObject string, DynamicObject other) {
            return true;
        }

        @Specialization(guards = {
                "string != other",
                "isRubyString(other)",
                "!areComparable(string, other, sameEncodingProfile, firstStringEmptyProfile, secondStringEmptyProfile, firstStringCR7BitProfile, secondStringCR7BitProfile, firstStringAsciiCompatible, secondStringAsciiCompatible)"
        })
        public boolean stringEqualNotComparable(DynamicObject string, DynamicObject other,
                                                @Cached("createBinaryProfile()") ConditionProfile sameEncodingProfile,
                                                @Cached("createBinaryProfile()") ConditionProfile firstStringEmptyProfile,
                                                @Cached("createBinaryProfile()") ConditionProfile secondStringEmptyProfile,
                                                @Cached("createBinaryProfile()") ConditionProfile firstStringCR7BitProfile,
                                                @Cached("createBinaryProfile()") ConditionProfile secondStringCR7BitProfile,
                                                @Cached("createBinaryProfile()") ConditionProfile firstStringAsciiCompatible,
                                                @Cached("createBinaryProfile()") ConditionProfile secondStringAsciiCompatible) {
            return false;
        }

        @Specialization(guards = {
                "string != other",
                "isRubyString(other)",
                "areComparable(string, other, sameEncodingProfile, firstStringEmptyProfile, secondStringEmptyProfile, firstStringCR7BitProfile, secondStringCR7BitProfile, firstStringAsciiCompatible, secondStringAsciiCompatible)"
        })
        public boolean equal(DynamicObject string, DynamicObject other,
                                 @Cached("createBinaryProfile()") ConditionProfile sameRopeProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile sameEncodingProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile firstStringEmptyProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile secondStringEmptyProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile firstStringCR7BitProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile secondStringCR7BitProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile firstStringAsciiCompatible,
                                 @Cached("createBinaryProfile()") ConditionProfile secondStringAsciiCompatible,
                                 @Cached("createBinaryProfile()") ConditionProfile differentSizeProfile) {

            final Rope a = Layouts.STRING.getRope(string);
            final Rope b = Layouts.STRING.getRope(other);

            // TODO (nirvdrum 08-Jan-16) Make this its own specialization to avoid the costly "areComparable" check.
            if (sameRopeProfile.profile(a == b)) {
                return true;
            }

            if (differentSizeProfile.profile(a.byteLength() != b.byteLength())) {
                return false;
            }

            return Arrays.equals(a.getBytes(), b.getBytes());
        }

        protected boolean areComparable(DynamicObject first, DynamicObject second,
                                      ConditionProfile sameEncodingProfile,
                                      ConditionProfile firstStringEmptyProfile,
                                      ConditionProfile secondStringEmptyProfile,
                                      ConditionProfile firstStringCR7BitProfile,
                                      ConditionProfile secondStringCR7BitProfile,
                                      ConditionProfile firstStringAsciiCompatible,
                                      ConditionProfile secondStringAsciiCompatible) {
            assert RubyGuards.isRubyString(first);
            assert RubyGuards.isRubyString(second);

            final Rope firstRope = Layouts.STRING.getRope(first);
            final Rope secondRope = Layouts.STRING.getRope(second);

            if (sameEncodingProfile.profile(firstRope.getEncoding() == secondRope.getEncoding())) {
                return true;
            }

            if (firstStringEmptyProfile.profile(firstRope.isEmpty())) {
                return true;
            }

            if (secondStringEmptyProfile.profile(secondRope.isEmpty())) {
                return true;
            }

            final int firstCodeRange = StringOperations.scanForCodeRange(first);
            final int secondCodeRange = StringOperations.scanForCodeRange(second);

            if (firstStringCR7BitProfile.profile(firstCodeRange == StringSupport.CR_7BIT)) {
                if (secondStringCR7BitProfile.profile(secondCodeRange == StringSupport.CR_7BIT)) {
                    return true;
                }

                if (secondStringAsciiCompatible.profile(secondRope.getEncoding().isAsciiCompatible())) {
                    return true;
                }
            }

            if (secondStringCR7BitProfile.profile(secondCodeRange == StringSupport.CR_7BIT)) {
                if (firstStringAsciiCompatible.profile(firstRope.getEncoding().isAsciiCompatible())) {
                    return true;
                }
            }

            return false;
        }
    }

    @RubiniusPrimitive(name = "string_find_character")
    @ImportStatic(StringGuards.class)
    public static abstract class StringFindCharacterNode extends RubiniusPrimitiveNode {

        @Child private TaintResultNode taintResultNode;
        @Child private AllocateObjectNode allocateObjectNode;

        public StringFindCharacterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = "isSingleByte(string)")
        public Object stringFindCharacterSingleByte(DynamicObject string, int offset) {
            // Taken from Rubinius's String::find_character.

            if (offset < 0) {
                return nil();
            }

            final ByteList byteList = StringOperations.getByteList(string);
            if (offset >= byteList.getRealSize()) {
                return nil();
            }

            final DynamicObject ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(new ByteList(byteList, offset, 1), StringSupport.CR_7BIT), null);

            return propagate(string, ret);
        }

        @Specialization(guards = "!isSingleByte(string)")
        public Object stringFindCharacter(DynamicObject string, int offset) {
            // Taken from Rubinius's String::find_character.

            if (offset < 0) {
                return nil();
            }

            final ByteList byteList = StringOperations.getByteList(string);
            if (offset >= byteList.getRealSize()) {
                return nil();
            }

            final ByteList bytes = byteList;
            final Encoding enc = bytes.getEncoding();
            final int clen = StringSupport.preciseLength(enc, bytes.getUnsafeBytes(), bytes.begin(), bytes.begin() + bytes.realSize());

            final DynamicObject ret;
            if (StringSupport.MBCLEN_CHARFOUND_P(clen)) {
                ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(new ByteList(byteList, offset, clen), StringSupport.CR_UNKNOWN), null);
            } else {
                ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(new ByteList(byteList, offset, 1), StringSupport.CR_7BIT), null);
            }

            return propagate(string, ret);
        }

        private Object propagate(DynamicObject string, DynamicObject ret) {
            StringOperations.getByteList(ret).setEncoding(Layouts.STRING.getRope(string).getEncoding());
            StringOperations.setCodeRange(ret, StringOperations.getCodeRange(string));
            return maybeTaint(string, ret);
        }

        private Object maybeTaint(DynamicObject source, DynamicObject value) {
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

        @Specialization(guards = {"isRubyEncoding(encoding)", "isSimple(code, encoding)"})
        public DynamicObject stringFromCodepointSimple(int code, DynamicObject encoding) {
            return createString(new ByteList(new byte[]{(byte) code}, EncodingOperations.getEncoding(encoding)));
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyEncoding(encoding)", "!isSimple(code, encoding)"})
        public DynamicObject stringFromCodepoint(int code, DynamicObject encoding) {
            final int length;

            try {
                length = EncodingOperations.getEncoding(encoding).codeToMbcLength(code);
            } catch (EncodingException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(code, encoding, this));
            }

            if (length <= 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(code, encoding, this));
            }

            final byte[] bytes = new byte[length];

            try {
                EncodingOperations.getEncoding(encoding).codeToMbc(code, bytes, 0);
            } catch (EncodingException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(code, encoding, this));
            }

            return createString(new ByteList(bytes, EncodingOperations.getEncoding(encoding)));
        }

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject stringFromCodepointSimple(long code, DynamicObject encoding) {
            if (code < Integer.MIN_VALUE || code > Integer.MAX_VALUE) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }

            return stringFromCodepointSimple((int) code, encoding);
        }

        protected boolean isSimple(int code, DynamicObject encoding) {
            return EncodingOperations.getEncoding(encoding) == ASCIIEncoding.INSTANCE && code >= 0x00 && code <= 0xFF;
        }

    }

    @RubiniusPrimitive(name = "string_to_f", needsSelf = false)
    public static abstract class StringToFPrimitiveNode extends RubiniusPrimitiveNode {

        public StringToFPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object stringToF(DynamicObject string) {
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
        public Object stringIndex(VirtualFrame frame, DynamicObject string, DynamicObject pattern, int start) {
            if (byteIndexToCharIndexNode == null) {
                CompilerDirectives.transferToInterpreter();
                byteIndexToCharIndexNode = insert(StringPrimitiveNodesFactory.StringByteCharacterIndexNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
            }

            // Rubinius will pass in a byte index for the `start` value, but StringSupport.index requires a character index.
            final int charIndex = byteIndexToCharIndexNode.executeStringBytCharacterIndex(frame, string, start, 0);

            final int index = StringSupport.index(StringOperations.getCodeRangeableReadOnly(string),
                    StringOperations.getCodeRangeableReadOnly(pattern),
                    charIndex, Layouts.STRING.getRope(string).getEncoding());

            if (index == -1) {
                return nil();
            }

            return index;
        }

    }

    @RubiniusPrimitive(name = "string_character_byte_index", needsSelf = false, lowerFixnumParameters = { 0, 1 })
    @ImportStatic(StringGuards.class)
    public static abstract class CharacterByteIndexNode extends RubiniusPrimitiveNode {

        public CharacterByteIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int stringCharacterByteIndex(DynamicObject string, int index, int start) {
            return start + index;
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public int stringCharacterByteIndexMultiByteEncoding(DynamicObject string, int index, int start) {
            final ByteList bytes = StringOperations.getByteList(string);

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

        public abstract int executeStringBytCharacterIndex(VirtualFrame frame, DynamicObject string, int index, int start);

        @Specialization(guards = "isSingleByteOptimizableOrAsciiOnly(string)")
        public int stringByteCharacterIndexSingleByte(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index;
        }

        @Specialization(guards = { "!isSingleByteOptimizableOrAsciiOnly(string)", "isFixedWidthEncoding(string)", "!isValidUtf8(string)" })
        public int stringByteCharacterIndexFixedWidth(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index / Layouts.STRING.getRope(string).getEncoding().minLength();
        }

        @Specialization(guards = { "!isSingleByteOptimizableOrAsciiOnly(string)", "!isFixedWidthEncoding(string)", "isValidUtf8(string)" })
        public int stringByteCharacterIndexValidUtf8(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.

            // TODO (nirvdrum 02-Apr-15) There's a way to optimize this for UTF-8, but porting all that code isn't necessary at the moment.
            return stringByteCharacterIndex(string, index, start);
        }

        @TruffleBoundary
        @Specialization(guards = { "!isSingleByteOptimizableOrAsciiOnly(string)", "!isFixedWidthEncoding(string)", "!isValidUtf8(string)" })
        public int stringByteCharacterIndex(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index and Encoding::find_byte_character_index.

            final ByteList bytes = StringOperations.getByteList(string);
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
        public Object stringCharacterIndex(DynamicObject string, DynamicObject pattern, int offset) {
            if (offset < 0) {
                return nil();
            }

            final Rope stringRope = Layouts.STRING.getRope(string);
            final int total = stringRope.byteLength();
            int p = StringOperations.getByteListReadOnly(string).getBegin();
            final int e = p + total;
            int pp = StringOperations.getByteListReadOnly(pattern).getBegin();
            final int pe = pp + Layouts.STRING.getRope(pattern).byteLength();
            int s;
            int ss;

            final byte[] stringBytes = StringOperations.getByteListReadOnly(string).getUnsafeBytes();
            final byte[] patternBytes = StringOperations.getByteListReadOnly(pattern).getUnsafeBytes();

            if (stringRope.isSingleByteOptimizable()) {
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

            final Encoding enc = Layouts.STRING.getRope(string).getEncoding();
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

    @RubiniusPrimitive(name = "string_byte_index", needsSelf = false, lowerFixnumParameters = { 0, 1 })
    @ImportStatic(StringGuards.class)
    public static abstract class StringByteIndexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringByteIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public Object stringByteIndexSingleByte(DynamicObject string, int index, int start,
                                                @Cached("createBinaryProfile()") ConditionProfile indexTooLargeProfile) {
            if (indexTooLargeProfile.profile(Layouts.STRING.getRope(string).byteLength() < index)) {
                return nil();
            }

            return index;
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public Object stringByteIndex(DynamicObject string, int index, int start,
                                      @Cached("createBinaryProfile()") ConditionProfile indexTooLargeProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile invalidByteProfile) {
            // Taken from Rubinius's String::byte_index.

            final ByteList bytes = StringOperations.getByteListReadOnly(string);

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
                if(invalidByteProfile.profile(! StringSupport.MBCLEN_CHARFOUND_P(c))) {
                    ++p;
                } else {
                    p += StringSupport.MBCLEN_CHARFOUND_LEN(c);
                }
            }

            if (indexTooLargeProfile.profile(i < k)) {
                return nil();
            } else {
                return p - bytes.begin();
            }
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringByteIndex(DynamicObject string, DynamicObject pattern, int offset) {
            // Taken from Rubinius's String::byte_index.

            final int match_size = Layouts.STRING.getRope(pattern).byteLength();

            if (offset < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative start given", this));
            }

            if (match_size == 0) return offset;

            if (StringOperations.scanForCodeRange(string) == StringSupport.CR_BROKEN) {
                return nil();
            }

            final ByteList stringByteList = StringOperations.getByteList(string);
            final ByteList patternByteList = StringOperations.getByteList(pattern);

            final Encoding encoding = StringOperations.checkEncoding(getContext(), string, StringOperations.getCodeRangeable(pattern), this);
            int p = stringByteList.getBegin();
            final int e = p + stringByteList.getRealSize();
            int pp = patternByteList.getBegin();
            final int pe = pp + patternByteList.getRealSize();
            int s;
            int ss;

            final byte[] stringBytes = stringByteList.getUnsafeBytes();
            final byte[] patternBytes = patternByteList.getUnsafeBytes();

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
                        return s - stringByteList.begin();
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
        public Object stringPreviousByteIndex(DynamicObject string, int index) {
            // Port of Rubinius's String::previous_byte_index.

            if (index < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative index given", this));
            }

            final ByteList bytes = StringOperations.getByteListReadOnly(string);
            final int p = bytes.getBegin();
            final int end = p + bytes.getRealSize();

            final int b = bytes.getEncoding().prevCharHead(bytes.getUnsafeBytes(), p, p + index, end);

            if (b == -1) {
                return nil();
            }

            return b - p;
        }

    }

    @RubiniusPrimitive(name = "string_copy_from", needsSelf = false, lowerFixnumParameters = { 2, 3, 4 })
    public static abstract class StringCopyFromPrimitiveNode extends RubiniusPrimitiveNode {

        public StringCopyFromPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isRubyString(other)", "size >= 0", "!offsetTooLarge(start, other)", "!offsetTooLargeRaw(dest, string)" })
        public DynamicObject stringCopyFrom(DynamicObject string, DynamicObject other, int start, int size, int dest,
                                            @Cached("createBinaryProfile()") ConditionProfile negativeStartOffsetProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile sizeTooLargeInReplacementProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile negativeDestinationOffsetProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile sizeTooLargeInStringProfile) {
            // Taken from Rubinius's String::copy_from.

            int src = start;
            int dst = dest;
            int cnt = size;

            final ByteList otherBytes = StringOperations.getByteListReadOnly(other);
            int osz = otherBytes.length();
            if(negativeStartOffsetProfile.profile(src < 0)) src = 0;
            if(sizeTooLargeInReplacementProfile.profile(cnt > osz - src)) cnt = osz - src;

            final ByteList stringBytes = Layouts.STRING.getRope(string).toByteListCopy();
            int sz = stringBytes.unsafeBytes().length - stringBytes.begin();
            if(negativeDestinationOffsetProfile.profile(dst < 0)) dst = 0;
            if(sizeTooLargeInStringProfile.profile(cnt > sz - dst)) cnt = sz - dst;

            System.arraycopy(otherBytes.unsafeBytes(), otherBytes.begin() + src, stringBytes.getUnsafeBytes(), stringBytes.begin() + dest, cnt);

            Layouts.STRING.setRope(string, StringOperations.ropeFromByteList(stringBytes));

            return string;
        }

        @Specialization(guards = { "isRubyString(other)", "size < 0 || (offsetTooLarge(start, other) || offsetTooLargeRaw(dest, string))" })
        public DynamicObject stringCopyFromWithNegativeSize(DynamicObject string, DynamicObject other, int start, int size, int dest) {
            return string;
        }

        protected boolean offsetTooLarge(int offset, DynamicObject string) {
            assert RubyGuards.isRubyString(string);

            return offset >= Layouts.STRING.getRope(string).byteLength();
        }

        protected boolean offsetTooLargeRaw(int offset, DynamicObject string) {
            assert RubyGuards.isRubyString(string);

            // This bounds checks on the total capacity rather than the virtual
            // size() of the String. This allows for string adjustment within
            // the capacity without having to change the virtual size first.
            final ByteList byteList = StringOperations.getByteListReadOnly(string);
            return offset >= (byteList.unsafeBytes().length - byteList.begin());
        }

    }

    @RubiniusPrimitive(name = "string_resize_capacity", needsSelf = false, lowerFixnumParameters = 1)
    public static abstract class StringResizeCapacityPrimitiveNode extends RubiniusPrimitiveNode {

        public StringResizeCapacityPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject stringResizeCapacity(DynamicObject string, int capacity) {
            StringOperations.getByteList(string).ensure(capacity);
            return string;
        }

    }

    @RubiniusPrimitive(name = "string_rindex", lowerFixnumParameters = 1)
    public static abstract class StringRindexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringRindexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringRindex(DynamicObject string, DynamicObject pattern, int start) {
            // Taken from Rubinius's String::rindex.

            int pos = start;

            if (pos < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative start given", this));
            }

            final ByteList buf = StringOperations.getByteListReadOnly(string);
            final int total = buf.getRealSize();
            final int matchSize = Layouts.STRING.getRope(pattern).byteLength();

            if (pos >= total) {
                pos = total - 1;
            }

            switch(matchSize) {
                case 0: {
                    return start;
                }

                case 1: {
                    final int matcher = StringOperations.getByteListReadOnly(pattern).get(0);

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
                        if (ByteList.memcmp(StringOperations.getByteListReadOnly(string).getUnsafeBytes(), cur, StringOperations.getByteListReadOnly(pattern).getUnsafeBytes(), 0, matchSize) == 0) {
                            return cur;
                        }

                        cur--;
                    }
                }
            }

            return nil();
        }

    }

    @RubiniusPrimitive(name = "string_pattern", lowerFixnumParameters = { 0, 1 })
    public static abstract class StringPatternPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public StringPatternPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = "value == 0")
        public DynamicObject stringPatternZero(DynamicObject stringClass, int size, int value) {
            ByteList bytes = new ByteList(new byte[size]);
            return allocateObjectNode.allocate(stringClass, StringOperations.ropeFromByteList(bytes, StringSupport.CR_UNKNOWN), null);
        }

        @Specialization(guards = "value != 0")
        public DynamicObject stringPattern(DynamicObject stringClass, int size, int value) {
            final byte[] bytes = new byte[size];
            Arrays.fill(bytes, (byte) value);
            return allocateObjectNode.allocate(stringClass, StringOperations.ropeFromByteList(new ByteList(bytes), StringSupport.CR_UNKNOWN), null);
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject stringPattern(DynamicObject stringClass, int size, DynamicObject string) {
            final byte[] bytes = new byte[size];
            final ByteList byteList = StringOperations.getByteListReadOnly(string);

            if (byteList.length() > 0) {
                for (int n = 0; n < size; n += byteList.length()) {
                    System.arraycopy(byteList.unsafeBytes(), byteList.begin(), bytes, n, Math.min(byteList.length(), size - n));
                }
            }

            return allocateObjectNode.allocate(stringClass, StringOperations.ropeFromByteList(new ByteList(bytes), StringSupport.CR_UNKNOWN), null);
        }

    }

    @RubiniusPrimitive(name = "string_to_inum")
    public static abstract class StringToInumPrimitiveNode extends RubiniusPrimitiveNode {

        public StringToInumPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object stringToInum(DynamicObject string, int fixBase, boolean strict) {
            try {
                final org.jruby.RubyInteger result = ConvertBytes.byteListToInum19(getContext().getRuntime(),
                        StringOperations.getByteListReadOnly(string),
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
        public DynamicObject stringByteAppend(DynamicObject string, DynamicObject other) {
            StringOperations.getByteList(string).append(StringOperations.getByteList(other));
            return string;
        }

    }

    @RubiniusPrimitive(name = "string_substring", lowerFixnumParameters = { 0, 1 })
    @ImportStatic(StringGuards.class)
    public static abstract class StringSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateNode;
        @Child private TaintResultNode taintResultNode;

        public StringSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object execute(VirtualFrame frame, DynamicObject string, int beg, int len);

        @Specialization(guards = { "len >= 0" , "isSingleByteOptimizable(string)" })
        public Object stringSubstring(DynamicObject string, int beg, int len,
                                                           @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile,
                                                           @Cached("createBinaryProfile()") ConditionProfile tooLargeBeginProfile,
                                                           @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                                                           @Cached("createBinaryProfile()") ConditionProfile stillNegativeBeginProfile,
                                                           @Cached("createBinaryProfile()") ConditionProfile tooLargeTotalProfile,
                                                           @Cached("createBinaryProfile()") ConditionProfile negativeLengthProfile) {
            // Taken from org.jruby.RubyString#substr19.
            final int length = StringOperations.byteLength(string);
            if (emptyStringProfile.profile(length == 0)) {
                len = 0;
            }

            if (tooLargeBeginProfile.profile(beg > length)) {
                return nil();
            }

            if (negativeBeginProfile.profile(beg < 0)) {
                beg += length;

                if (stillNegativeBeginProfile.profile(beg < 0)) {
                    return nil();
                }
            }

            if (tooLargeTotalProfile.profile((beg + len) > length)) {
                len = length - beg;
            }

            if (negativeLengthProfile.profile(len <= 0)) {
                len = 0;
                beg = 0;
            }

            return makeRope(string, beg, len);
        }

        @TruffleBoundary
        @Specialization(guards = { "len >= 0", "!isSingleByteOptimizable(string)" })
        public Object stringSubstring(DynamicObject string, int beg, int len) {
            // Taken from org.jruby.RubyString#substr19 & org.jruby.RubyString#multibyteSubstr19.

            final int length = StringOperations.byteLength(string);
            if (length == 0) {
                len = 0;
            }

            if ((beg + len) > length) {
                len = length - beg;
            }

            final Rope rope = Layouts.STRING.getRope(string);
            final Encoding enc = rope.getEncoding();
            int p;
            int s = 0;
            int end = s + length;
            byte[]bytes = rope.getBytes();

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
                    beg += StringSupport.strLengthFromRubyString(StringOperations.getCodeRangeable(string), enc);
                    if (beg < 0) {
                        return nil();
                    }
                }
            } else if (beg > 0 && beg > StringSupport.strLengthFromRubyString(StringOperations.getCodeRangeable(string), enc)) {
                return nil();
            }
            if (len == 0) {
                p = 0;
            } else if (StringOperations.isCodeRangeValid(string) && enc instanceof UTF8Encoding) {
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

        @Specialization(guards = "len < 0")
        public Object stringSubstringNegativeLength(DynamicObject string, int beg, int len) {
            return nil();
        }

        // TODO (nirvdrum 08-Jan-16) Remove this.
        private DynamicObject makeSubstring(DynamicObject string, int beg, int len) {
            assert RubyGuards.isRubyString(string);

            if (allocateNode == null) {
                CompilerDirectives.transferToInterpreter();
                allocateNode = insert(AllocateObjectNodeGen.create(getContext(), getSourceSection(), null, null));
            }

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            final DynamicObject ret = allocateNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(string),
                    new ByteList(StringOperations.getByteList(string), beg, len),
                    StringOperations.getCodeRange(string) == StringSupport.CR_7BIT ? StringSupport.CR_7BIT : StringSupport.CR_UNKNOWN,
                    null);

            taintResultNode.maybeTaint(string, ret);

            return ret;
        }

        private DynamicObject makeRope(DynamicObject string, int beg, int len) {
            assert RubyGuards.isRubyString(string);

            if (allocateNode == null) {
                CompilerDirectives.transferToInterpreter();
                allocateNode = insert(AllocateObjectNodeGen.create(getContext(), getSourceSection(), null, null));
            }

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            // TODO (nirvdrum Nov. 9, 2015) There's probably a way to guarantee the code range value based on the parent code range.
            final DynamicObject ret = allocateNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(string),
                    new SubstringRope(Layouts.STRING.getRope(string), beg, len),
                    null);

            taintResultNode.maybeTaint(string, ret);

            return ret;
        }

    }

    @RubiniusPrimitive(name = "string_from_bytearray", needsSelf = false, lowerFixnumParameters = { 1, 2 })
    public static abstract class StringFromByteArrayPrimitiveNode extends RubiniusPrimitiveNode {

        public StringFromByteArrayPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubiniusByteArray(bytes)")
        public DynamicObject stringFromByteArray(DynamicObject bytes, int start, int count) {
            // Data is copied here - can we do something COW?
            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(bytes);
            return createString(new ByteList(byteList, start, count));
        }

    }

}
