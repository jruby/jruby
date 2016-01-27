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
import org.jruby.truffle.nodes.core.EncodingNodes;
import org.jruby.truffle.nodes.core.RopeNodes;
import org.jruby.truffle.nodes.core.RopeNodesFactory;
import org.jruby.truffle.nodes.core.StringGuards;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.EncodingOperations;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.RopeOperations;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.StringSupport;
import static org.jruby.truffle.runtime.core.StringOperations.encoding;
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

    @RubiniusPrimitive(name = "string_append")
    public static abstract class StringAppendPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private RopeNodes.MakeConcatNode makeConcatNode;

        public StringAppendPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
        }

        public abstract DynamicObject executeStringAppend(VirtualFrame frame, DynamicObject string, DynamicObject other);

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject stringAppend(VirtualFrame frame, DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            final Encoding compatibleEncoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, other);

            if (compatibleEncoding == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().encodingCompatibilityError(
                        String.format("incompatible encodings: %s and %s", left.getEncoding(), right.getEncoding()), this));
            }

            Layouts.STRING.setRope(string, makeConcatNode.executeMake(left, right, compatibleEncoding));

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_awk_split")
    public static abstract class StringAwkSplitPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public StringAwkSplitPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject stringAwkSplit(DynamicObject string, int lim) {
            final List<DynamicObject> ret = new ArrayList<>();
            final Rope rope = rope(string);
            final ByteList value = rope.getUnsafeByteList();
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
            final boolean singlebyte = rope.isSingleByteOptimizable();
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

            final Rope rope = makeSubstringNode.executeMake(rope(source), index, length);

            final DynamicObject ret = Layouts.STRING.createString(Layouts.CLASS.getInstanceFactory(Layouts.BASIC_OBJECT.getLogicalClass(source)), rope, null);
            taintResultNode.maybeTaint(source, ret);

            return ret;
        }
    }

    @RubiniusPrimitive(name = "string_byte_substring")
    public static abstract class StringByteSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public StringByteSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, int index, NotProvided length) {
            final DynamicObject subString = (DynamicObject) stringByteSubstring(frame, string, index, 1);

            if (subString == nil()) {
                return subString;
            }

            if (rope(subString).isEmpty()) {
                return nil();
            }

            return subString;
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, DynamicObject string, int index, int length) {
            if (length < 0) {
                return nil();
            }

            final Rope rope = rope(string);
            final int stringLength = rope.characterLength();
            final int normalizedIndex = StringOperations.normalizeIndex(stringLength, index);

            if (normalizedIndex < 0 || normalizedIndex > rope.byteLength()) {
                return nil();
            }

            if (normalizedIndex + length > rope.byteLength()) {
                length = rope.byteLength() - normalizedIndex;
            }

            final Rope substringRope = makeSubstringNode.executeMake(rope, normalizedIndex, length);
            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), substringRope, null);

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

    }

    @RubiniusPrimitive(name = "string_check_null_safe", needsSelf = false)
    public static abstract class StringCheckNullSafePrimitiveNode extends RubiniusPrimitiveNode {

        public StringCheckNullSafePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject stringCheckNullSafe(DynamicObject string) {
            final byte[] bytes = rope(string).getBytes();

            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] == 0) {
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

        public StringCompareSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(other)")
        public int stringCompareSubstring(VirtualFrame frame, DynamicObject string, DynamicObject other, int start, int size) {
            // Transliterated from Rubinius C++.

            final int stringLength = StringOperations.rope(string).characterLength();
            final int otherLength = StringOperations.rope(other).characterLength();

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

            final Rope rope = StringOperations.rope(string);
            final Rope otherRope = StringOperations.rope(other);

            // TODO (nirvdrum 21-Jan-16): Reimplement with something more friendly to rope byte[] layout?
            return ByteList.memcmp(rope.getBytes(), rope.getBegin(), size,
                    otherRope.getBytes(), otherRope.getBegin() + start, size);
        }

    }

    @RubiniusPrimitive(name = "string_equal", needsSelf = true)
    public static abstract class StringEqualPrimitiveNode extends RubiniusPrimitiveNode {

        public StringEqualPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract boolean executeStringEqual(DynamicObject string, DynamicObject other);

        @Specialization(guards = "ropeEqual(string, other)")
        public boolean stringEqualsRopeEquals(DynamicObject string, DynamicObject other) {
            return true;
        }

        @Specialization(guards = {
                "isRubyString(other)",
                "!ropeEqual(string, other)",
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
                "isRubyString(other)",
                "!ropeEqual(string, other)",
                "areComparable(string, other, sameEncodingProfile, firstStringEmptyProfile, secondStringEmptyProfile, firstStringCR7BitProfile, secondStringCR7BitProfile, firstStringAsciiCompatible, secondStringAsciiCompatible)"
        })
        public boolean equal(DynamicObject string, DynamicObject other,
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

            if (differentSizeProfile.profile(a.byteLength() != b.byteLength())) {
                return false;
            }

            return a.equals(b);
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

            final int firstCodeRange = firstRope.getCodeRange();
            final int secondCodeRange = secondRope.getCodeRange();

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

        protected static boolean ropeEqual(DynamicObject first, DynamicObject second) {
            assert RubyGuards.isRubyString(first);
            assert RubyGuards.isRubyString(second);

            return rope(first) == rope(second);
        }
    }

    @RubiniusPrimitive(name = "string_find_character")
    @ImportStatic(StringGuards.class)
    public static abstract class StringFindCharacterNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public StringFindCharacterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization(guards = "offset < 0")
        public Object stringFindCharacterNegativeOffset(DynamicObject string, int offset) {
            return nil();
        }

        @Specialization(guards = { "offset >= 0", "isSingleByte(string)" })
        public Object stringFindCharacterSingleByte(DynamicObject string, int offset,
                                                    @Cached("createBinaryProfile()") ConditionProfile offsetTooLargeProfile) {
            // Taken from Rubinius's String::find_character.

            final Rope rope = rope(string);
            if (offsetTooLargeProfile.profile(offset >= rope.byteLength())) {
                return nil();
            }

            final DynamicObject ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), makeSubstringNode.executeMake(rope, offset, 1), null);

            return propagate(string, ret);
        }

        @Specialization(guards = { "offset >= 0", "!isSingleByte(string)" })
        public Object stringFindCharacter(DynamicObject string, int offset,
                                          @Cached("createBinaryProfile()") ConditionProfile offsetTooLargeProfile) {
            // Taken from Rubinius's String::find_character.

            final Rope rope = rope(string);
            if (offsetTooLargeProfile.profile(offset >= rope.byteLength())) {
                return nil();
            }

            final Encoding enc = rope.getEncoding();
            final int clen = StringSupport.preciseLength(enc, rope.getBytes(), rope.begin(), rope.begin() + rope.realSize());

            final DynamicObject ret;
            if (StringSupport.MBCLEN_CHARFOUND_P(clen)) {
                ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), makeSubstringNode.executeMake(rope, offset, clen), null);
            } else {
                // TODO (nirvdrum 13-Jan-16) We know that the code range is CR_7BIT. Ensure we're not wasting time figuring that out again in the substring creation.
                ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), makeSubstringNode.executeMake(rope, offset, 1), null);
            }

            return propagate(string, ret);
        }

        private Object propagate(DynamicObject string, DynamicObject ret) {
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

        public abstract int executeInt(VirtualFrame frame, DynamicObject string, int index, int start);

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int stringCharacterByteIndex(DynamicObject string, int index, int start) {
            return start + index;
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public int stringCharacterByteIndexMultiByteEncoding(DynamicObject string, int index, int start) {
            final Rope rope = rope(string);

            return StringSupport.nth(rope.getEncoding(), rope.getBytes(), start, rope.byteLength(), index);
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
            return index / encoding(string).minLength();
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

            final Rope rope = rope(string);
            final byte[] bytes = rope.getBytes();
            final Encoding encoding = rope.getEncoding();
            int p = start;
            final int end = bytes.length;
            int charIndex = 0;

            while (p < end && index > 0) {
                final int charLen = StringSupport.length(encoding, bytes, p, end);
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

            final Rope stringRope = rope(string);
            final Rope patternRope = rope(pattern);

            final int total = stringRope.byteLength();
            int p = stringRope.begin();
            final int e = p + total;
            int pp = patternRope.begin();
            final int pe = pp + patternRope.byteLength();
            int s;
            int ss;

            final byte[] stringBytes = stringRope.getBytes();
            final byte[] patternBytes = patternRope.getBytes();

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

            final Encoding enc = stringRope.getEncoding();
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
            if (indexTooLargeProfile.profile(index > rope(string).byteLength())) {
                return nil();
            }

            return index;
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public Object stringByteIndex(DynamicObject string, int index, int start,
                                      @Cached("createBinaryProfile()") ConditionProfile indexTooLargeProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile invalidByteProfile) {
            // Taken from Rubinius's String::byte_index.

            final Rope rope = rope(string);
            final Encoding enc = rope.getEncoding();
            int p = rope.getBegin();
            final int e = p + rope.getRealSize();

            int i, k = index;

            if (k < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("character index is negative", this));
            }

            for (i = 0; i < k && p < e; i++) {
                final int c = StringSupport.preciseLength(enc, rope.getBytes(), p, e);

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
                return p - rope.begin();
            }
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringByteIndex(DynamicObject string, DynamicObject pattern, int offset,
                                      @Cached("createBinaryProfile()") ConditionProfile emptyPatternProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile) {
            // Taken from Rubinius's String::byte_index.

            if (offset < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative start given", this));
            }

            final Rope stringRope = rope(string);
            final Rope patternRope = rope(pattern);

            if (emptyPatternProfile.profile(patternRope.isEmpty())) return offset;

            if (brokenCodeRangeProfile.profile(stringRope.getCodeRange() == StringSupport.CR_BROKEN)) {
                return nil();
            }

            final Encoding encoding = StringOperations.checkEncoding(getContext(), string, pattern, this);
            int p = stringRope.getBegin();
            final int e = p + stringRope.getRealSize();
            int pp = patternRope.getBegin();
            final int pe = pp + patternRope.getRealSize();
            int s;
            int ss;

            final byte[] stringBytes = stringRope.getBytes();
            final byte[] patternBytes = patternRope.getBytes();

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
                        return s - stringRope.begin();
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

            final Rope rope = rope(string);
            final int p = rope.getBegin();
            final int end = p + rope.getRealSize();

            final int b = rope.getEncoding().prevCharHead(rope.getBytes(), p, p + index, end);

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

            final Rope otherRope = rope(other);
            int osz = otherRope.byteLength();
            if(negativeStartOffsetProfile.profile(src < 0)) src = 0;
            if(sizeTooLargeInReplacementProfile.profile(cnt > osz - src)) cnt = osz - src;

            final ByteList stringBytes = Layouts.STRING.getRope(string).toByteListCopy();
            int sz = stringBytes.unsafeBytes().length - stringBytes.begin();
            if(negativeDestinationOffsetProfile.profile(dst < 0)) dst = 0;
            if(sizeTooLargeInStringProfile.profile(cnt > sz - dst)) cnt = sz - dst;

            System.arraycopy(otherRope.getBytes(), otherRope.begin() + src, stringBytes.getUnsafeBytes(), stringBytes.begin() + dest, cnt);

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

            // TODO (nirvdrum 21-Jan-16) Verify whether we still need this method as we never have spare capacity allocated with ropes.
            final Rope rope = rope(string);
            return offset >= (rope.byteLength() - rope.begin());
        }

    }

    @RubiniusPrimitive(name = "string_resize_capacity", needsSelf = false, lowerFixnumParameters = 1)
    public static abstract class StringResizeCapacityPrimitiveNode extends RubiniusPrimitiveNode {

        public StringResizeCapacityPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject stringResizeCapacity(DynamicObject string, int capacity) {
            // TODO (nirvdrum 11-Jan-16): Any calls to this are suspect now that we use ropes. We don't have a way to preallocate a buffer to mutate.
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

            final Rope stringRope = rope(string);
            final Rope patternRope = rope(pattern);
            final int total = stringRope.byteLength();
            final int matchSize = patternRope.byteLength();

            if (pos >= total) {
                pos = total - 1;
            }

            switch(matchSize) {
                case 0: {
                    return start;
                }

                case 1: {
                    final int matcher = patternRope.get(0);

                    while (pos >= 0) {
                        if (stringRope.get(pos) == matcher) {
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
                        // TODO (nirvdrum 21-Jan-16): Investigate a more rope efficient memcmp.
                        if (ByteList.memcmp(stringRope.getBytes(), cur, patternRope.getBytes(), 0, matchSize) == 0) {
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
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public StringPatternPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null);
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
            final Rope rope = rope(string);
            final byte[] bytes = new byte[size];

            // TODO (nirvdrum 21-Jan-16): Investigate whether using a ConcatRope would be better here.
            if (! rope.isEmpty()) {
                for (int n = 0; n < size; n += rope.byteLength()) {
                    System.arraycopy(rope.getBytes(), rope.begin(), bytes, n, Math.min(rope.byteLength(), size - n));
                }
            }

            // TODO (nirvdrum 21-Jan-16): Verify the encoding and code range are correct.
            return allocateObjectNode.allocate(stringClass, makeLeafRopeNode.executeMake(bytes, encoding(string), StringSupport.CR_UNKNOWN), null);
        }

    }

    @RubiniusPrimitive(name = "string_splice", needsSelf = false, lowerFixnumParameters = {2, 3})
    public static abstract class StringSplicePrimitiveNode extends RubiniusPrimitiveNode {

        @Child private RopeNodes.MakeConcatNode appendMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode prependMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode leftMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode rightMakeConcatNode;
        @Child private RopeNodes.MakeSubstringNode prependMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode leftMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode rightMakeSubstringNode;

        public StringSplicePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "indexAtStartBound(spliceByteIndex)", "isRubyString(other)" })
        public Object splicePrepend(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace) {
            if (prependMakeSubstringNode == null) {
                CompilerDirectives.transferToInterpreter();
                prependMakeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            if (prependMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                prependMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final Rope original = rope(string);
            final Rope left = rope(other);
            final Rope right = prependMakeSubstringNode.executeMake(original, byteCountToReplace, original.byteLength() - byteCountToReplace);

            Layouts.STRING.setRope(string, prependMakeConcatNode.executeMake(left, right, right.getEncoding()));

            return string;
        }

        @Specialization(guards = { "indexAtEndBound(string, spliceByteIndex)", "isRubyString(other)" })
        public Object spliceAppend(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            if (appendMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                appendMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            Layouts.STRING.setRope(string, appendMakeConcatNode.executeMake(left, right, left.getEncoding()));

            return string;
        }

        @Specialization(guards = { "!indexAtEitherBounds(string, spliceByteIndex)", "isRubyString(other)" })
        public DynamicObject splice(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace) {
            if (leftMakeSubstringNode == null) {
                CompilerDirectives.transferToInterpreter();
                leftMakeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            if (rightMakeSubstringNode == null) {
                CompilerDirectives.transferToInterpreter();
                rightMakeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            if (leftMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                leftMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            if (rightMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                rightMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final Rope source = rope(string);
            final Rope insert = rope(other);
            final int rightSideStartingIndex = spliceByteIndex + byteCountToReplace;

            final Rope splitLeft = leftMakeSubstringNode.executeMake(source, 0, spliceByteIndex);
            final Rope splitRight = rightMakeSubstringNode.executeMake(source, rightSideStartingIndex, source.byteLength() - rightSideStartingIndex);
            final Rope joinedLeft = leftMakeConcatNode.executeMake(splitLeft, insert, source.getEncoding());
            final Rope joinedRight = rightMakeConcatNode.executeMake(joinedLeft, splitRight, source.getEncoding());

            Layouts.STRING.setRope(string, joinedRight);

            return string;
        }

        protected  boolean indexAtStartBound(int index) {
            return index == 0;
        }

        protected boolean indexAtEndBound(DynamicObject string, int index) {
            assert RubyGuards.isRubyString(string);

            return index == rope(string).byteLength();
        }

        protected boolean indexAtEitherBounds(DynamicObject string, int index) {
            assert RubyGuards.isRubyString(string);

            return indexAtStartBound(index) || indexAtEndBound(string, index);
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

        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public StringByteAppendPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject stringByteAppend(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            // The semantics of this primitive are such that the original string's byte[] should be extended without
            // any modification to the other properties of the string. This is counter-intuitive because adding bytes
            // from another string may very well change the code range for the source string. Updating the code range,
            // however, breaks other things so we can't do it. As an example, StringIO starts with an empty UTF-8
            // string and then appends ASCII-8BIT bytes, but must retain the original UTF-8 encoding. The binary contents
            // of the ASCII-8BIT string could give the resulting string a CR_BROKEN code range on UTF-8, but if we do
            // this, StringIO ceases to work -- the resulting string must retain the original CR_7BIT code range. It's
            // ugly, but seems to be due to a difference in how Rubinius keeps track of byte optimizable strings.

            final Rope rightConverted = makeLeafRopeNode.executeMake(right.getBytes(), left.getEncoding(), left.getCodeRange());

            Layouts.STRING.setRope(string, makeConcatNode.executeMake(left, rightConverted, left.getEncoding()));

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_substring", lowerFixnumParameters = { 0, 1 })
    @ImportStatic(StringGuards.class)
    public static abstract class StringSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
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
            final Rope rope = rope(string);
            if (emptyStringProfile.profile(rope.isEmpty())) {
                len = 0;
            }

            final int length = rope.byteLength();
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

            final Rope rope = rope(string);
            final int length = rope.byteLength();

            if (rope.isEmpty()) {
                len = 0;
            }

            if ((beg + len) > length) {
                len = length - beg;
            }

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
                    return makeRope(string, p - s, e - p);
                } else {
                    beg += StringSupport.strLengthFromRubyString(StringOperations.getCodeRangeableReadOnly(string), enc);
                    if (beg < 0) {
                        return nil();
                    }
                }
            } else if (beg > 0 && beg > StringSupport.strLengthFromRubyString(StringOperations.getCodeRangeableReadOnly(string), enc)) {
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
            return makeRope(string, p - s, len);
        }

        @Specialization(guards = "len < 0")
        public Object stringSubstringNegativeLength(DynamicObject string, int beg, int len) {
            return nil();
        }

        private DynamicObject makeRope(DynamicObject string, int beg, int len) {
            assert RubyGuards.isRubyString(string);

            if (allocateNode == null) {
                CompilerDirectives.transferToInterpreter();
                allocateNode = insert(AllocateObjectNodeGen.create(getContext(), getSourceSection(), null, null));
            }

            if (makeSubstringNode == null) {
                CompilerDirectives.transferToInterpreter();
                makeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            final DynamicObject ret = allocateNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(string),
                    makeSubstringNode.executeMake(rope(string), beg, len),
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
