/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.StringNodesFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.util.Arrays;

/**
 * Rubinius primitives associated with the Ruby {@code String} class.
 */
public abstract class StringPrimitiveNodes {

    @RubiniusPrimitive(name = "string_byte_substring")
    public static abstract class StringByteSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private TaintResultNode taintResultNode;

        public StringByteSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(context, sourceSection, true, 0, null);
        }

        public StringByteSubstringPrimitiveNode(StringByteSubstringPrimitiveNode prev) {
            super(prev);
            taintResultNode = prev.taintResultNode;
        }

        @Specialization
        public Object stringByteSubstring(RubyString string, int index, UndefinedPlaceholder length) {
            final Object subString = stringByteSubstring(string, index, 1);

            if (subString == getContext().getCoreLibrary().getNilObject()) {
                return subString;
            }

            if (((RubyString) subString).getByteList().length() == 0) {
                return getContext().getCoreLibrary().getNilObject();
            }

            return subString;
        }

        @Specialization
        public Object stringByteSubstring(RubyString string, int index, int length) {
            final ByteList bytes = string.getBytes();

            if (length < 0) {
                return getContext().getCoreLibrary().getNilObject();
            }

            final int normalizedIndex = string.normalizeIndex(index);

            if (normalizedIndex > bytes.length()) {
                return getContext().getCoreLibrary().getNilObject();
            }

            int rangeEnd = normalizedIndex + length;
            if (rangeEnd > bytes.length()) {
                rangeEnd = bytes.length();
            }

            if (normalizedIndex < bytes.getBegin()) {
                return getContext().getCoreLibrary().getNilObject();
            }

            final byte[] copiedBytes = Arrays.copyOfRange(bytes.getUnsafeBytes(), normalizedIndex, rangeEnd);
            final RubyString result = new RubyString(getContext().getCoreLibrary().getStringClass(), new ByteList(copiedBytes, string.getBytes().getEncoding()));

            return taintResultNode.maybeTaint(string, result);
        }

        @Specialization
        public Object stringByteSubstring(RubyString string, int index, double length) {
            return stringByteSubstring(string, index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(RubyString string, double index, UndefinedPlaceholder length) {
            return stringByteSubstring(string, (int) index, 1);
        }

        @Specialization
        public Object stringByteSubstring(RubyString string, double index, double length) {
            return stringByteSubstring(string, (int) index, (int) length);
        }

        @Specialization
        public Object stringByteSubstring(RubyString string, double index, int length) {
            return stringByteSubstring(string, (int) index, length);
        }

        @Specialization
        public Object stringByteSubstring(RubyString string, RubyRange range, UndefinedPlaceholder unused) {
            return null;
        }

        @Specialization(guards = "!isRubyRange(arguments[1])")
        public Object stringByteSubstring(RubyString string, Object indexOrRange, Object length) {
            return null;
        }

    }

    @RubiniusPrimitive(name = "string_check_null_safe", needsSelf = false)
    public static abstract class StringCheckNullSafePrimitiveNode extends RubiniusPrimitiveNode {

        private final ConditionProfile nullByteProfile = ConditionProfile.createBinaryProfile();

        public StringCheckNullSafePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StringCheckNullSafePrimitiveNode(StringCheckNullSafePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public boolean stringCheckNullSafe(RubyString string) {
            for (byte b : string.getBytes().unsafeBytes()) {
                if (nullByteProfile.profile(b == 0)) {
                    return false;
                }
            }

            return true;
        }

    }

    @RubiniusPrimitive(name = "string_equal", needsSelf = true)
    public static abstract class StringEqualPrimitiveNode extends RubiniusPrimitiveNode {

        private final ConditionProfile incompatibleEncodingProfile = ConditionProfile.createBinaryProfile();

        public StringEqualPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StringEqualPrimitiveNode(StringEqualPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public boolean stringEqual(RubyString string, RubyString other) {
            final ByteList a = string.getBytes();
            final ByteList b = other.getBytes();

            if (incompatibleEncodingProfile.profile((a.getEncoding() != b.getEncoding()) &&
                    (org.jruby.RubyEncoding.areCompatible(string, other) == null))) {
                return false;
            }

            return a.equal(b);
        }

    }

    @RubiniusPrimitive(name = "string_find_character")
    public static abstract class StringFindCharacterPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private StringNodes.GetIndexNode getIndexNode;

        public StringFindCharacterPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getIndexNode = StringNodesFactory.GetIndexNodeFactory.create(context, sourceSection, new RubyNode[]{});
        }

        public StringFindCharacterPrimitiveNode(StringFindCharacterPrimitiveNode prev) {
            super(prev);
            getIndexNode = prev.getIndexNode;
        }

        @Specialization
        public Object stringFindCharacter(RubyString string, int index) {
            return getIndexNode.getIndex(string, index, UndefinedPlaceholder.INSTANCE);
        }

    }

    @RubiniusPrimitive(name = "string_from_codepoint", needsSelf = false)
    public static abstract class StringFromCodepointPrimitiveNode extends RubiniusPrimitiveNode {

        public StringFromCodepointPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StringFromCodepointPrimitiveNode(StringFromCodepointPrimitiveNode prev) {
            super(prev);
        }

        @Specialization(guards = "isSimple")
        public RubyString stringFromCodepointSimple(int code, RubyEncoding encoding) {
            return new RubyString(
                    getContext().getCoreLibrary().getStringClass(),
                    new ByteList(new byte[]{(byte) code}, encoding.getEncoding()));
        }

        @Specialization(guards = "!isSimple")
        public RubyString stringFromCodepoint(int code, RubyEncoding encoding) {
            notDesignedForCompilation();

            final int length;

            try {
                length = encoding.getEncoding().codeToMbcLength(code);
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
                encoding.getEncoding().codeToMbc(code, bytes, 0);
            } catch (EncodingException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(code, encoding, this));
            }

            return new RubyString(
                    getContext().getCoreLibrary().getStringClass(),
                    new ByteList(bytes, encoding.getEncoding()));
        }

        @Specialization
        public RubyString stringFromCodepointSimple(long code, RubyEncoding encoding) {
            notDesignedForCompilation();

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

        public StringToFPrimitiveNode(StringToFPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object stringToF(RubyString string) {
            notDesignedForCompilation();

            try {
                return Double.parseDouble(string.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    @RubiniusPrimitive(name = "string_index")
    public static abstract class StringIndexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StringIndexPrimitiveNode(StringIndexPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object stringIndex(RubyString string, RubyString pattern, int start) {
            final int index = StringSupport.index(string,
                    pattern,
                    start, string.getBytes().getEncoding());

            if (index == -1) {
                return getContext().getCoreLibrary().getNilObject();
            }

            return index;
        }

    }

}
