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
 * Some of the code in this class is transposed from org.jruby.util.ByteList,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 *
 * Copyright (C) 2007-2010 JRuby Community
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.cast.ArrayAttributeCastNodeGen;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.encoding.EncodingNodes;
import org.jruby.truffle.core.encoding.EncodingOperations;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.RepeatingRope;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeBuffer;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringGuards;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.StringSupport;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.jruby.truffle.core.string.StringOperations.encoding;
import static org.jruby.truffle.core.string.StringOperations.rope;

/**
 * Rubinius primitives associated with the Ruby {@code String} class.
 */
public abstract class StringPrimitiveNodes {

    @RubiniusPrimitive(name = "character_ascii_p")
    @ImportStatic(StringGuards.class)
    public static abstract class CharacterAsciiPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
    public static abstract class CharacterPrintablePrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
    public static abstract class StringAppendPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private RopeNodes.MakeConcatNode makeConcatNode;

        public StringAppendPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
        }

        public abstract DynamicObject executeStringAppend(DynamicObject string, DynamicObject other);

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject stringAppend(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            final Encoding compatibleEncoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, other);

            if (compatibleEncoding == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().encodingCompatibilityError(
                        String.format("incompatible encodings: %s and %s", left.getEncoding(), right.getEncoding()), this));
            }

            StringOperations.setRope(string, makeConcatNode.executeMake(left, right, compatibleEncoding));

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_awk_split")
    public static abstract class StringAwkSplitPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
            final boolean limit = lim > 0;
            int i = lim > 0 ? 1 : 0;

            byte[]bytes = rope.getBytes();
            int p = rope.begin();
            int ptr = p;
            int len = rope.byteLength();
            int end = p + len;
            Encoding enc = rope.getEncoding();
            boolean skip = true;

            int e = 0, b = 0;
            final boolean singlebyte = rope.isSingleByteOptimizable();
            while (p < end) {
                final int c;
                if (singlebyte) {
                    c = bytes[p++] & 0xff;
                } else {
                    try {
                        c = StringSupport.codePoint(getContext().getJRubyRuntime(), enc, bytes, p, end);
                    } catch (org.jruby.exceptions.RaiseException ex) {
                        throw new RaiseException(getContext().getJRubyInterop().toTruffle(ex.getException(), this));
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
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), objects, objects.length);
        }

        private DynamicObject makeString(DynamicObject source, int index, int length) {
            assert RubyGuards.isRubyString(source);

            final Rope rope = makeSubstringNode.executeMake(rope(source), index, length);

            final DynamicObject ret = Layouts.STRING.createString(Layouts.CLASS.getInstanceFactory(Layouts.BASIC_OBJECT.getLogicalClass(source)), rope);
            taintResultNode.maybeTaint(source, ret);

            return ret;
        }
    }

    @RubiniusPrimitive(name = "string_byte_substring")
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "string"),
            @NodeChild(type = RubyNode.class, value = "index"),
            @NodeChild(type = RubyNode.class, value = "length")
    })
    public static abstract class StringByteSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public static StringByteSubstringPrimitiveNode create(RubyContext context, SourceSection sourceSection) {
            return StringPrimitiveNodesFactory.StringByteSubstringPrimitiveNodeFactory.create(context, sourceSection, null, null, null);
        }

        public StringByteSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ArrayAttributeCastNodeGen.create(getContext(), getSourceSection(), "index", index);
        }

        @CreateCast("length") public RubyNode coerceLengthToInt(RubyNode length) {
            return ArrayAttributeCastNodeGen.create(getContext(), getSourceSection(), "length", length);
        }

        public Object executeStringByteSubstring(DynamicObject string, Object index, Object length) { return nil(); }

        @Specialization
        public Object stringByteSubstring(DynamicObject string, int index, NotProvided length,
                                          @Cached("createBinaryProfile()") ConditionProfile negativeLengthProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile indexOutOfBoundsProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile lengthTooLongProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile nilSubstringProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile emptySubstringProfile) {
            final DynamicObject subString = (DynamicObject) stringByteSubstring(string, index, 1, negativeLengthProfile, indexOutOfBoundsProfile, lengthTooLongProfile);

            if (nilSubstringProfile.profile(subString == nil())) {
                return subString;
            }

            if (emptySubstringProfile.profile(rope(subString).isEmpty())) {
                return nil();
            }

            return subString;
        }

        @Specialization
        public Object stringByteSubstring(DynamicObject string, int index, int length,
                                          @Cached("createBinaryProfile()") ConditionProfile negativeLengthProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile indexOutOfBoundsProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile lengthTooLongProfile) {
            if (negativeLengthProfile.profile(length < 0)) {
                return nil();
            }

            final Rope rope = rope(string);
            final int stringLength = rope.characterLength();
            final int normalizedIndex = StringOperations.normalizeIndex(stringLength, index);

            if (indexOutOfBoundsProfile.profile(normalizedIndex < 0 || normalizedIndex > rope.byteLength())) {
                return nil();
            }

            if (lengthTooLongProfile.profile(normalizedIndex + length > rope.byteLength())) {
                length = rope.byteLength() - normalizedIndex;
            }

            final Rope substringRope = makeSubstringNode.executeMake(rope, normalizedIndex, length);
            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), substringRope, null);

            return taintResultNode.maybeTaint(string, result);
        }

        @Specialization(guards = "isRubyRange(range)")
        public Object stringByteSubstring(DynamicObject string, DynamicObject range, NotProvided length) {
            return null;
        }

    }

    @RubiniusPrimitive(name = "string_check_null_safe", needsSelf = false)
    public static abstract class StringCheckNullSafePrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        public StringCheckNullSafePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject stringCheckNullSafe(DynamicObject string) {
            final byte[] bytes = rope(string).getBytes();

            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] == 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().argumentError("string contains NULL byte", this));
                }
            }

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_chr_at", lowerFixnumParameters = 0)
    @ImportStatic(StringGuards.class)
    public static abstract class StringChrAtPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        public StringChrAtPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "indexOutOfBounds(string, byteIndex)")
        public Object stringChrAtOutOfBounds(DynamicObject string, int byteIndex) {
            return false;
        }

        @Specialization(guards = { "!indexOutOfBounds(string, byteIndex)", "isSingleByteOptimizable(string)" })
        public Object stringChrAtSingleByte(DynamicObject string, int byteIndex,
                                            @Cached("create(getContext(), getSourceSection())") StringByteSubstringPrimitiveNode stringByteSubstringNode) {
            return stringByteSubstringNode.executeStringByteSubstring(string, byteIndex, 1);
        }

        @Specialization(guards = { "!indexOutOfBounds(string, byteIndex)", "!isSingleByteOptimizable(string)" })
        public Object stringChrAt(DynamicObject string, int byteIndex,
                                  @Cached("create(getContext(), getSourceSection())") StringByteSubstringPrimitiveNode stringByteSubstringNode) {
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

            return stringByteSubstringNode.executeStringByteSubstring(string, byteIndex, n);
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
    public static abstract class StringCompareSubstringPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
                        coreLibrary().indexError(
                                String.format("index %d out of string", start),
                                this
                        ));
            }

            if (start < 0) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        coreLibrary().indexError(
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
            return ByteList.memcmp(rope.getBytes(), rope.begin(), size,
                    otherRope.getBytes(), otherRope.begin() + start, size);
        }

    }

    @RubiniusPrimitive(name = "string_equal", needsSelf = true)
    @ImportStatic(StringGuards.class)
    public static abstract class StringEqualPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        public StringEqualPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract boolean executeStringEqual(DynamicObject string, DynamicObject other);

        @Specialization(guards = "ropeReferenceEqual(string, other)")
        public boolean stringEqualsRopeEquals(DynamicObject string, DynamicObject other) {
            return true;
        }

        @Specialization(guards = {
                "isRubyString(other)",
                "!ropeReferenceEqual(string, other)",
                "bytesReferenceEqual(string, other)"
        })
        public boolean stringEqualsBytesEquals(DynamicObject string, DynamicObject other) {
            return true;
        }

        @Specialization(guards = {
                "isRubyString(other)",
                "!ropeReferenceEqual(string, other)",
                "!bytesReferenceEqual(string, other)",
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
                "!ropeReferenceEqual(string, other)",
                "!bytesReferenceEqual(string, other)",
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

            final CodeRange firstCodeRange = firstRope.getCodeRange();
            final CodeRange secondCodeRange = secondRope.getCodeRange();

            if (firstStringCR7BitProfile.profile(firstCodeRange == CodeRange.CR_7BIT)) {
                if (secondStringCR7BitProfile.profile(secondCodeRange == CodeRange.CR_7BIT)) {
                    return true;
                }

                if (secondStringAsciiCompatible.profile(secondRope.getEncoding().isAsciiCompatible())) {
                    return true;
                }
            }

            if (secondStringCR7BitProfile.profile(secondCodeRange == CodeRange.CR_7BIT)) {
                if (firstStringAsciiCompatible.profile(firstRope.getEncoding().isAsciiCompatible())) {
                    return true;
                }
            }

            return false;
        }

        protected static boolean ropeReferenceEqual(DynamicObject first, DynamicObject second) {
            assert RubyGuards.isRubyString(first);
            assert RubyGuards.isRubyString(second);

            return rope(first) == rope(second);
        }

        protected static boolean bytesReferenceEqual(DynamicObject first, DynamicObject second) {
            assert RubyGuards.isRubyString(first);
            assert RubyGuards.isRubyString(second);

            final Rope firstRope = rope(first);
            final Rope secondRope = rope(second);

            return firstRope.getCodeRange() == CodeRange.CR_7BIT &&
                    secondRope.getCodeRange() == CodeRange.CR_7BIT &&
                    firstRope.getRawBytes() != null &&
                    firstRope.getRawBytes() == secondRope.getRawBytes();
        }
    }

    @RubiniusPrimitive(name = "string_find_character")
    @ImportStatic(StringGuards.class)
    public static abstract class StringFindCharacterNode extends RubiniusPrimitiveArrayArgumentsNode {

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
            final int clen = StringSupport.preciseLength(enc, rope.getBytes(), rope.begin(), rope.begin() + rope.byteLength());

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
    public static abstract class StringFromCodepointPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        public StringFromCodepointPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyEncoding(encoding)", "isSimple(code, encoding)"})
        public DynamicObject stringFromCodepointSimple(int code, DynamicObject encoding,
                                                       @Cached("createBinaryProfile()") ConditionProfile isUTF8Profile,
                                                       @Cached("createBinaryProfile()") ConditionProfile isUSAsciiProfile,
                                                       @Cached("createBinaryProfile()") ConditionProfile isAscii8BitProfile) {
            final Encoding realEncoding = EncodingOperations.getEncoding(encoding);
            final Rope rope;

            if (isUTF8Profile.profile(realEncoding == UTF8Encoding.INSTANCE)) {
                rope = RopeConstants.UTF8_SINGLE_BYTE_ROPES[code];
            } else if (isUSAsciiProfile.profile(realEncoding == USASCIIEncoding.INSTANCE)) {
                rope = RopeConstants.US_ASCII_SINGLE_BYTE_ROPES[code];
            } else if (isAscii8BitProfile.profile(realEncoding == ASCIIEncoding.INSTANCE)) {
                rope = RopeConstants.ASCII_8BIT_SINGLE_BYTE_ROPES[code];
            } else {
                rope = RopeOperations.create(new byte[] { (byte) code }, realEncoding, CodeRange.CR_UNKNOWN);
            }

            return createString(rope);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyEncoding(encoding)", "!isSimple(code, encoding)"})
        public DynamicObject stringFromCodepoint(int code, DynamicObject encoding) {
            final int length;

            try {
                length = EncodingOperations.getEncoding(encoding).codeToMbcLength(code);
            } catch (EncodingException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().rangeError(code, encoding, this));
            }

            if (length <= 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().rangeError(code, encoding, this));
            }

            final byte[] bytes = new byte[length];

            try {
                EncodingOperations.getEncoding(encoding).codeToMbc(code, bytes, 0);
            } catch (EncodingException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().rangeError(code, encoding, this));
            }

            return createString(new ByteList(bytes, EncodingOperations.getEncoding(encoding)));
        }

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject stringFromCodepointSimple(long code, DynamicObject encoding,
                                                       @Cached("createBinaryProfile()") ConditionProfile isUTF8Profile,
                                                       @Cached("createBinaryProfile()") ConditionProfile isUSAsciiProfile,
                                                       @Cached("createBinaryProfile()") ConditionProfile isAscii8BitProfile) {
            if (code < Integer.MIN_VALUE || code > Integer.MAX_VALUE) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }

            return stringFromCodepointSimple((int) code, encoding, isUTF8Profile, isUSAsciiProfile, isAscii8BitProfile);
        }

        protected boolean isSimple(int code, DynamicObject encoding) {
            return EncodingOperations.getEncoding(encoding) == ASCIIEncoding.INSTANCE && code >= 0x00 && code <= 0xFF;
        }

    }

    @RubiniusPrimitive(name = "string_to_f", needsSelf = false)
    public static abstract class StringToFPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
    @ImportStatic(StringGuards.class)
    public static abstract class StringIndexPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child StringByteCharacterIndexNode byteIndexToCharIndexNode;

        public StringIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isRubyString(pattern)", "isBrokenCodeRange(pattern)" })
        public DynamicObject stringIndexBrokenCodeRange(DynamicObject string, DynamicObject pattern, int start) {
            return nil();
        }


        @Specialization(guards = { "isRubyString(pattern)", "!isBrokenCodeRange(pattern)" })
        public Object stringIndex(VirtualFrame frame, DynamicObject string, DynamicObject pattern, int start) {
            if (byteIndexToCharIndexNode == null) {
                CompilerDirectives.transferToInterpreter();
                byteIndexToCharIndexNode = insert(StringPrimitiveNodesFactory.StringByteCharacterIndexNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
            }

            // Rubinius will pass in a byte index for the `start` value, but StringSupport.index requires a character index.
            final int charIndex = byteIndexToCharIndexNode.executeStringBytCharacterIndex(frame, string, start, 0);

            final int index = index(rope(string), rope(pattern), charIndex, encoding(string));

            if (index == -1) {
                return nil();
            }

            return index;
        }

        @TruffleBoundary
        private int index(Rope source, Rope other, int offset, Encoding enc) {
            // Taken from org.jruby.util.StringSupport.index.

            int sourceLen = source.characterLength();
            int otherLen = other.characterLength();

            if (offset < 0) {
                offset += sourceLen;
                if (offset < 0) return -1;
            }

            if (sourceLen - offset < otherLen) return -1;
            byte[]bytes = source.getBytes();
            int p = source.begin();
            int end = p + source.byteLength();
            if (offset != 0) {
                offset = source.isSingleByteOptimizable() ? offset : StringSupport.offset(enc, bytes, p, end, offset);
                p += offset;
            }
            if (otherLen == 0) return offset;

            while (true) {
                int pos = indexOf(source, other, p - source.begin());
                if (pos < 0) return pos;
                pos -= (p - source.begin());
                int t = enc.rightAdjustCharHead(bytes, p, p + pos, end);
                if (t == p + pos) return pos + offset;
                if ((sourceLen -= t - p) <= 0) return -1;
                offset += t - p;
                p = t;
            }
        }

        @TruffleBoundary
        private int indexOf(Rope sourceRope, Rope otherRope, int fromIndex) {
            // Taken from org.jruby.util.ByteList.indexOf.

            final byte[] source = sourceRope.getBytes();
            final int sourceOffset = sourceRope.begin();
            final int sourceCount = sourceRope.byteLength();
            final byte[] target = otherRope.getBytes();
            final int targetOffset = otherRope.begin();
            final int targetCount = otherRope.byteLength();

            if (fromIndex >= sourceCount) return (targetCount == 0 ? sourceCount : -1);
            if (fromIndex < 0) fromIndex = 0;
            if (targetCount == 0) return fromIndex;

            byte first  = target[targetOffset];
            int max = sourceOffset + (sourceCount - targetCount);

            for (int i = sourceOffset + fromIndex; i <= max; i++) {
                if (source[i] != first) while (++i <= max && source[i] != first);

                if (i <= max) {
                    int j = i + 1;
                    int end = j + targetCount - 1;
                    for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);

                    if (j == end) return i - sourceOffset;
                }
            }
            return -1;
        }
    }

    @RubiniusPrimitive(name = "string_character_byte_index", needsSelf = false, lowerFixnumParameters = { 0, 1 })
    @ImportStatic(StringGuards.class)
    public static abstract class CharacterByteIndexNode extends RubiniusPrimitiveArrayArgumentsNode {

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
    public static abstract class StringByteCharacterIndexNode extends RubiniusPrimitiveArrayArgumentsNode {

        public StringByteCharacterIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract int executeStringBytCharacterIndex(VirtualFrame frame, DynamicObject string, int index, int start);

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int stringByteCharacterIndexSingleByte(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index;
        }

        @Specialization(guards = { "!isSingleByteOptimizable(string)", "isFixedWidthEncoding(string)" })
        public int stringByteCharacterIndexFixedWidth(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index / encoding(string).minLength();
        }

        @Specialization(guards = { "!isSingleByteOptimizable(string)", "!isFixedWidthEncoding(string)", "isValidUtf8(string)" })
        public int stringByteCharacterIndexValidUtf8(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.

            // TODO (nirvdrum 02-Apr-15) There's a way to optimize this for UTF-8, but porting all that code isn't necessary at the moment.
            return stringByteCharacterIndex(string, index, start);
        }

        @TruffleBoundary
        @Specialization(guards = { "!isSingleByteOptimizable(string)", "!isFixedWidthEncoding(string)", "!isValidUtf8(string)" })
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
    public static abstract class StringCharacterIndexPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
    public static abstract class StringByteIndexPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
            int p = rope.begin();
            final int e = p + rope.byteLength();

            int i, k = index;

            if (k < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError("character index is negative", this));
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
                throw new RaiseException(coreLibrary().argumentError("negative start given", this));
            }

            final Rope stringRope = rope(string);
            final Rope patternRope = rope(pattern);

            if (emptyPatternProfile.profile(patternRope.isEmpty())) return offset;

            if (brokenCodeRangeProfile.profile(stringRope.getCodeRange() == CodeRange.CR_BROKEN)) {
                return nil();
            }

            final Encoding encoding = StringOperations.checkEncoding(getContext(), string, pattern, this);
            int p = stringRope.begin();
            final int e = p + stringRope.byteLength();
            int pp = patternRope.begin();
            final int pe = pp + patternRope.byteLength();
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

    // Port of Rubinius's String::previous_byte_index.
    //
    // This method takes a byte index, finds the corresponding character the byte index belongs to, and then returns
    // the byte index marking the start of the previous character in the string.
    @RubiniusPrimitive(name = "string_previous_byte_index")
    @ImportStatic(StringGuards.class)
    public static abstract class StringPreviousByteIndexPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        public StringPreviousByteIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "index < 0")
        @TruffleBoundary
        public Object stringPreviousByteIndexNegativeIndex(DynamicObject string, int index) {
            throw new RaiseException(coreLibrary().argumentError("negative index given", this));
        }

        @Specialization(guards = "index == 0")
        public Object stringPreviousByteIndexZeroIndex(DynamicObject string, int index) {
            return nil();
        }

        @Specialization(guards = { "index > 0", "isSingleByteOptimizable(string)" })
        public int stringPreviousByteIndexSingleByteOptimizable(DynamicObject string, int index) {
            return index - 1;
        }

        @Specialization(guards = { "index > 0", "!isSingleByteOptimizable(string)", "isFixedWidthEncoding(string)" })
        public int stringPreviousByteIndexFixedWidthEncoding(DynamicObject string, int index,
                                                             @Cached("createBinaryProfile()") ConditionProfile firstCharacterProfile) {
            final Encoding encoding = encoding(string);

            // TODO (nirvdrum 11-Apr-16) Determine whether we need to be bug-for-bug compatible with Rubinius.
            // Implement a bug in Rubinius. We already special-case the index == 0 by returning nil. For all indices
            // corresponding to a given character, we treat them uniformly. However, for the first character, we only
            // return nil if the index is 0. If any other index into the first character is encountered, we return 0.
            // It seems unlikely this will ever be encountered in practice, but it's here for completeness.
            if (firstCharacterProfile.profile(index < encoding.maxLength())) {
                return 0;
            }

            return (index / encoding.maxLength() - 1) * encoding.maxLength();
        }

        @Specialization(guards = { "index > 0", "!isSingleByteOptimizable(string)", "!isFixedWidthEncoding(string)" })
        @TruffleBoundary
        public Object stringPreviousByteIndex(DynamicObject string, int index) {
            final Rope rope = rope(string);
            final int p = rope.begin();
            final int end = p + rope.byteLength();

            final int b = rope.getEncoding().prevCharHead(rope.getBytes(), p, p + index, end);

            if (b == -1) {
                return nil();
            }

            return b - p;
        }

    }

    @RubiniusPrimitive(name = "string_copy_from", needsSelf = false, lowerFixnumParameters = { 2, 3, 4 })
    public static abstract class StringCopyFromPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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

            final ByteList stringBytes = RopeOperations.toByteListCopy(Layouts.STRING.getRope(string));
            int sz = stringBytes.unsafeBytes().length - stringBytes.begin();
            if(negativeDestinationOffsetProfile.profile(dst < 0)) dst = 0;
            if(sizeTooLargeInStringProfile.profile(cnt > sz - dst)) cnt = sz - dst;

            System.arraycopy(otherRope.getBytes(), otherRope.begin() + src, stringBytes.getUnsafeBytes(), stringBytes.begin() + dest, cnt);

            StringOperations.setRope(string, StringOperations.ropeFromByteList(stringBytes));

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

    @RubiniusPrimitive(name = "string_rindex", lowerFixnumParameters = 1)
    public static abstract class StringRindexPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private RopeNodes.GetByteNode patternGetByteNode;
        @Child private RopeNodes.GetByteNode stringGetByteNode;

        public StringRindexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            patternGetByteNode = RopeNodes.GetByteNode.create(context, sourceSection);
            stringGetByteNode = RopeNodes.GetByteNode.create(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringRindex(DynamicObject string, DynamicObject pattern, int start) {
            // Taken from Rubinius's String::rindex.

            int pos = start;

            if (pos < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError("negative start given", this));
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
                    final int matcher = patternGetByteNode.executeGetByte(patternRope, 0);

                    while (pos >= 0) {
                        if (stringGetByteNode.executeGetByte(stringRope, pos) == matcher) {
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
    public static abstract class StringPatternPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;
        @Child private RopeNodes.MakeRepeatingNode makeRepeatingNode;

        public StringPatternPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
            makeLeafRopeNode = RopeNodes.MakeLeafRopeNode.create(context, sourceSection);
            makeRepeatingNode = RopeNodes.MakeRepeatingNode.create(context, sourceSection);
        }

        @Specialization(guards = "value >= 0")
        public DynamicObject stringPatternZero(DynamicObject stringClass, int size, int value) {
            final Rope repeatingRope = makeRepeatingNode.executeMake(RopeConstants.ASCII_8BIT_SINGLE_BYTE_ROPES[value], size);

            return allocateObjectNode.allocate(stringClass, repeatingRope, null);
        }

        @Specialization(guards = { "isRubyString(string)", "patternFitsEvenly(string, size)" })
        public DynamicObject stringPatternFitsEvenly(DynamicObject stringClass, int size, DynamicObject string) {
            final Rope rope = rope(string);
            final Rope repeatingRope = makeRepeatingNode.executeMake(rope, size / rope.byteLength());

            return allocateObjectNode.allocate(stringClass, repeatingRope, null);
        }

        @Specialization(guards = { "isRubyString(string)", "!patternFitsEvenly(string, size)" })
        @TruffleBoundary
        public DynamicObject stringPattern(DynamicObject stringClass, int size, DynamicObject string) {
            final Rope rope = rope(string);
            final byte[] bytes = new byte[size];

            // TODO (nirvdrum 21-Jan-16): Investigate whether using a ConcatRope (potentially combined with a RepeatingRope) would be better here.
            if (! rope.isEmpty()) {
                for (int n = 0; n < size; n += rope.byteLength()) {
                    System.arraycopy(rope.getBytes(), rope.begin(), bytes, n, Math.min(rope.byteLength(), size - n));
                }
            }

            // If we reach this specialization, the `size` attribute will cause a truncated `string` to appear at the
            // end of the resulting string in order to pad the value out. A truncated CR_7BIT string is always CR_7BIT.
            // A truncated CR_VALID string could be any of the code range values.
            final CodeRange codeRange = rope.getCodeRange() == CodeRange.CR_7BIT ? CodeRange.CR_7BIT : CodeRange.CR_UNKNOWN;
            final Object characterLength = codeRange == CodeRange.CR_7BIT ? size : NotProvided.INSTANCE;

            return allocateObjectNode.allocate(stringClass, makeLeafRopeNode.executeMake(bytes, encoding(string), codeRange, characterLength), null);
        }

        protected boolean patternFitsEvenly(DynamicObject string, int size) {
            assert RubyGuards.isRubyString(string);

            final int byteLength = rope(string).byteLength();

            return byteLength > 0 && (size % byteLength) == 0;
        }

    }

    @RubiniusPrimitive(name = "string_splice", needsSelf = false, lowerFixnumParameters = {2, 3})
    @ImportStatic(StringGuards.class)
    public static abstract class StringSplicePrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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

        @Specialization(guards = { "indexAtStartBound(spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)" })
        public Object splicePrepend(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding) {
            if (prependMakeSubstringNode == null) {
                CompilerDirectives.transferToInterpreter();
                prependMakeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            if (prependMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                prependMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope original = rope(string);
            final Rope left = rope(other);
            final Rope right = prependMakeSubstringNode.executeMake(original, byteCountToReplace, original.byteLength() - byteCountToReplace);

            StringOperations.setRope(string, prependMakeConcatNode.executeMake(left, right, encoding));

            return string;
        }

        @Specialization(guards = { "indexAtEndBound(string, spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)" })
        public Object spliceAppend(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding) {
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope left = rope(string);
            final Rope right = rope(other);

            if (appendMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                appendMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            StringOperations.setRope(string, appendMakeConcatNode.executeMake(left, right, encoding));

            return string;
        }

        @Specialization(guards = { "!indexAtEitherBounds(string, spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)", "!isRopeBuffer(string)" })
        public DynamicObject splice(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding) {
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

            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope source = rope(string);
            final Rope insert = rope(other);
            final int rightSideStartingIndex = spliceByteIndex + byteCountToReplace;

            final Rope splitLeft = leftMakeSubstringNode.executeMake(source, 0, spliceByteIndex);
            final Rope splitRight = rightMakeSubstringNode.executeMake(source, rightSideStartingIndex, source.byteLength() - rightSideStartingIndex);
            final Rope joinedLeft = leftMakeConcatNode.executeMake(splitLeft, insert, encoding);
            final Rope joinedRight = rightMakeConcatNode.executeMake(joinedLeft, splitRight, encoding);

            StringOperations.setRope(string, joinedRight);

            return string;
        }

        @Specialization(guards = { "!indexAtEitherBounds(string, spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)", "isRopeBuffer(string)", "isSingleByteOptimizable(string)" })
        public DynamicObject spliceBuffer(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding,
                                          @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile) {
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final RopeBuffer source = (RopeBuffer) rope(string);
            final Rope insert = rope(other);
            final int rightSideStartingIndex = spliceByteIndex + byteCountToReplace;

            final ByteList byteList = new ByteList(source.byteLength() + insert.byteLength() - byteCountToReplace);

            byteList.append(source.getByteList(), 0, spliceByteIndex);
            byteList.append(insert.getBytes());
            byteList.append(source.getByteList(), rightSideStartingIndex, source.byteLength() - rightSideStartingIndex);
            byteList.setEncoding(encoding);

            final Rope buffer = new RopeBuffer(byteList,
                    RopeNodes.MakeConcatNode.commonCodeRange(source.getCodeRange(), insert.getCodeRange(), sameCodeRangeProfile, brokenCodeRangeProfile),
                    source.isSingleByteOptimizable() && insert.isSingleByteOptimizable(),
                    source.characterLength() + insert.characterLength() - byteCountToReplace);

            StringOperations.setRope(string, buffer);

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

        protected boolean isRopeBuffer(DynamicObject string) {
            assert RubyGuards.isRubyString(string);

            return rope(string) instanceof RopeBuffer;
        }
    }

    @RubiniusPrimitive(name = "string_to_inum")
    public static abstract class StringToInumPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        public StringToInumPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object stringToInum(DynamicObject string, int fixBase, boolean strict) {
            try {
                final org.jruby.RubyInteger result = ConvertBytes.byteListToInum19(getContext().getJRubyRuntime(),
                        StringOperations.getByteListReadOnly(string),
                        fixBase,
                        strict);

                return toTruffle(result);
            } catch (org.jruby.exceptions.RaiseException e) {
                throw new RaiseException(getContext().getJRubyInterop().toTruffle(e.getException(), this));
            }
        }

        private Object toTruffle(IRubyObject object) {
            if (object instanceof org.jruby.RubyFixnum) {
                final long value = ((org.jruby.RubyFixnum) object).getLongValue();

                if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                    return value;
                }

                return (int) value;
            } else if (object instanceof org.jruby.RubyBignum) {
                final BigInteger value = ((org.jruby.RubyBignum) object).getBigIntegerValue();
                return Layouts.BIGNUM.createBignum(coreLibrary().getBignumFactory(), value);
            } else {
                throw new UnsupportedOperationException();
            }
        }

    }

    @RubiniusPrimitive(name = "string_byte_append")
    public static abstract class StringByteAppendPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public StringByteAppendPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
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

            final Rope rightConverted = makeLeafRopeNode.executeMake(right.getBytes(), left.getEncoding(), left.getCodeRange(), NotProvided.INSTANCE);

            StringOperations.setRope(string, makeConcatNode.executeMake(left, rightConverted, left.getEncoding()));

            return string;
        }

    }

    @RubiniusPrimitive(name = "string_substring", lowerFixnumParameters = { 0, 1 })
    @ImportStatic(StringGuards.class)
    public static abstract class StringSubstringPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
                                                           @Cached("createBinaryProfile()") ConditionProfile negativeLengthProfile,
                                                           @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile) {
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

            if (mutableRopeProfile.profile(rope instanceof RopeBuffer)) {
                return makeBuffer(string, beg, len);
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
                    beg += rope.characterLength();
                    if (beg < 0) {
                        return nil();
                    }
                }
            } else if (beg > 0 && beg > rope.characterLength()) {
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

        private DynamicObject makeBuffer(DynamicObject string, int beg, int len) {
            assert RubyGuards.isRubyString(string);

            if (!StringGuards.isSingleByteOptimizable(string)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().internalError("Taking the substring of MBC rope buffer is not currently supported", this));
            }

            final RopeBuffer buffer = (RopeBuffer) rope(string);

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
                    new RopeBuffer(new ByteList(buffer.getByteList(), beg, len), buffer.getCodeRange(), buffer.isSingleByteOptimizable(), len),
                    null);

            taintResultNode.maybeTaint(string, ret);

            return ret;
        }

    }

    @RubiniusPrimitive(name = "string_from_bytearray", needsSelf = false, lowerFixnumParameters = { 1, 2 })
    public static abstract class StringFromByteArrayPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
