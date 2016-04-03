/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.read.bytes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.MissingValue;
import org.jruby.truffle.core.format.exceptions.InvalidFormatException;
import org.jruby.truffle.core.format.read.SourceNode;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadUTF8CharacterNode extends FormatNode {

    private final ConditionProfile rangeProfile = ConditionProfile.createBinaryProfile();

    public ReadUTF8CharacterNode(RubyContext context) {
        super(context);
    }

    @Specialization(guards = "isNull(source)")
    public void read(VirtualFrame frame, Object source) {
        advanceSourcePosition(frame, 1);
        throw new IllegalStateException();
    }

    @Specialization
    public Object read(VirtualFrame frame, byte[] source) {
        final int index = getSourcePosition(frame);
        final int sourceLength = getSourceLength(frame);

        assert index != -1;

        if (rangeProfile.profile(index >= sourceLength)) {
            return MissingValue.INSTANCE;
        }

        long codepoint = source[index] & 0xff;
        final int length;

        if (codepoint >> 7 == 0) {
            length = 1;
            codepoint &= 0b01111111;
        } else if (codepoint >> 5 == 0b00000110) {
            length = 2;
            codepoint &= 0b00011111;
        } else if (codepoint >> 4 == 0b00001110) {
            length = 3;
            codepoint &= 0b00001111;
        } else if (codepoint >> 3 == 0b00011110) {
            length = 4;
            codepoint &= 0b00000111;
        } else if (codepoint >> 2 == 0b00111110) {
            length = 5;
            codepoint &= 0b00000011;
        } else if (codepoint >> 1 == 0b01111110) {
            length = 6;
            codepoint &= 0b00000001;
        } else {
            // Not UTF-8, so just pass the first byte through
            length = 1;
        }

        if (index + length > sourceLength) {
            CompilerDirectives.transferToInterpreter();
            throw new InvalidFormatException(
                    String.format("malformed UTF-8 character (expected %d bytes, given %d bytes)",
                            length, sourceLength - index));
        }

        for (int n = 1; n < length; n++) {
            codepoint <<= 6;
            codepoint |= source[index + n] & 0b00111111;
        }

        setSourcePosition(frame, index + length);

        return codepoint;
    }

}
