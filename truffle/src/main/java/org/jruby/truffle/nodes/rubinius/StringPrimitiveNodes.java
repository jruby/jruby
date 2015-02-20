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
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.StringNodesFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * Rubinius primitives associated with the Ruby {@code String} class.
 */
public abstract class StringPrimitiveNodes {

    @RubiniusPrimitive(name = "string_byte_substring")
    public static abstract class StringByteSubstringPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private StringNodes.ByteSliceNode byteSliceNode;

        public StringByteSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            byteSliceNode = StringNodesFactory.ByteSliceNodeFactory.create(context, sourceSection, new RubyNode[] {});
        }

        public StringByteSubstringPrimitiveNode(StringByteSubstringPrimitiveNode prev) {
            super(prev);
            byteSliceNode = prev.byteSliceNode;
        }

        @Specialization
        public Object stringByteSubstring(VirtualFrame frame, RubyString string, int index, int length) {
            return byteSliceNode.byteSlice(string, index, length);
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

        @Specialization(guards = "isSimple(code, encoding)")
        public RubyString stringFromCodepointSimple(int code, RubyEncoding encoding) {
            return new RubyString(
                    getContext().getCoreLibrary().getStringClass(),
                    new ByteList(new byte[]{(byte) code}, encoding.getEncoding()));
        }

        @Specialization(guards = "!isSimple(code, encoding)")
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
