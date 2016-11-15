/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some logic copied from jruby.util.Pack
 *
 *  * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Derek Berner <derek.berner@state.nm.us>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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
package org.jruby.truffle.core.format.write.bytes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.exceptions.RangeException;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.string.UTF8Operations;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class WriteUTF8CharacterNode extends FormatNode {

    public WriteUTF8CharacterNode(RubyContext context) {
        super(context);
    }

    @Specialization(guards = {"value >= 0", "value <= 0x7f"})
    public Object writeSingleByte(VirtualFrame frame, long value,
                                  @Cached("createBinaryProfile()") ConditionProfile rangeProfile) {
        writeByte(frame, (byte) value);

        if (rangeProfile.profile(UTF8Operations.isUTF8ValidOneByte((byte) value))) {
            setStringCodeRange(frame, CodeRange.CR_7BIT);
        } else {
            setStringCodeRange(frame, CodeRange.CR_BROKEN);
        }

        return null;
    }

    @Specialization(guards = {"value > 0x7f", "value <= 0x7ff"})
    public Object writeTwoBytes(VirtualFrame frame, long value,
                                @Cached("createBinaryProfile()") ConditionProfile rangeProfile) {
        final byte[] bytes = {
                (byte) (((value >>> 6) & 0xff) | 0xc0),
                (byte) ((value & 0x3f) | 0x80)
        };

        writeBytes(frame, bytes);
        increaseStringLength(frame, -2 + 1);

        if (rangeProfile.profile(UTF8Operations.isUTF8ValidTwoBytes(bytes))) {
            setStringCodeRange(frame, CodeRange.CR_VALID);
        } else {
            setStringCodeRange(frame, CodeRange.CR_BROKEN);
        }

        return null;
    }

    @Specialization(guards = {"value > 0x7ff", "value <= 0xffff"})
    public Object writeThreeBytes(VirtualFrame frame, long value,
                                  @Cached("createBinaryProfile()") ConditionProfile rangeProfile) {
        final byte[] bytes = {
                (byte) (((value >>> 12) & 0xff) | 0xe0),
                (byte) (((value >>> 6) & 0x3f) | 0x80),
                (byte) ((value & 0x3f) | 0x80)
        };

        writeBytes(frame, bytes);
        increaseStringLength(frame, -3 + 1);

        if (rangeProfile.profile(UTF8Operations.isUTF8ValidThreeBytes(bytes))) {
            setStringCodeRange(frame, CodeRange.CR_VALID);
        } else {
            setStringCodeRange(frame, CodeRange.CR_BROKEN);
        }

        return null;
    }

    @Specialization(guards = {"value > 0xffff", "value <= 0x1fffff"})
    public Object writeFourBytes(VirtualFrame frame, long value,
                                 @Cached("createBinaryProfile()") ConditionProfile rangeProfile) {
        final byte[] bytes = {
                (byte) (((value >>> 18) & 0xff) | 0xf0),
                (byte) (((value >>> 12) & 0x3f) | 0x80),
                (byte) (((value >>> 6) & 0x3f) | 0x80),
                (byte) ((value & 0x3f) | 0x80)
        };

        writeBytes(frame, bytes);
        increaseStringLength(frame, -4 + 1);

        if (rangeProfile.profile(UTF8Operations.isUTF8ValidFourBytes(bytes))) {
            setStringCodeRange(frame, CodeRange.CR_VALID);
        } else {
            setStringCodeRange(frame, CodeRange.CR_BROKEN);
        }

        return null;
    }

    @Specialization(guards = {"value > 0x1fffff", "value <= 0x3ffffff"})
    public Object writeFiveBytes(VirtualFrame frame, long value,
                                 @Cached("createBinaryProfile()") ConditionProfile rangeProfile) {
        final byte[] bytes = {
                (byte) (((value >>> 24) & 0xff) | 0xf8),
                (byte) (((value >>> 18) & 0x3f) | 0x80),
                (byte) (((value >>> 12) & 0x3f) | 0x80),
                (byte) (((value >>> 6) & 0x3f) | 0x80),
                (byte) ((value & 0x3f) | 0x80)
        };

        writeBytes(frame, bytes);
        increaseStringLength(frame, -5 + 1);

        if (rangeProfile.profile(UTF8Operations.isUTF8ValidFiveBytes(bytes))) {
            setStringCodeRange(frame, CodeRange.CR_VALID);
        } else {
            setStringCodeRange(frame, CodeRange.CR_BROKEN);
        }

        return null;
    }

    @Specialization(guards = {"value > 0x3ffffff", "value <= 0x7fffffff"})
    public Object writeSixBytes(VirtualFrame frame, long value,
                                @Cached("createBinaryProfile()") ConditionProfile rangeProfile) {
        final byte[] bytes = {
                (byte) (((value >>> 30) & 0xff) | 0xfc),
                (byte) (((value >>> 24) & 0x3f) | 0x80),
                (byte) (((value >>> 18) & 0x3f) | 0x80),
                (byte) (((value >>> 12) & 0x3f) | 0x80),
                (byte) (((value >>> 6) & 0x3f) | 0x80),
                (byte) ((value & 0x3f) | 0x80)
        };

        writeBytes(frame, bytes);
        increaseStringLength(frame, -6 + 1);

        if (rangeProfile.profile(UTF8Operations.isUTF8ValidSixBytes(bytes))) {
            setStringCodeRange(frame, CodeRange.CR_VALID);
        } else {
            setStringCodeRange(frame, CodeRange.CR_BROKEN);
        }

        return null;
    }

    @Specialization(guards = "value < 0")
    public Object writeNegative(long value) {
        throw new RangeException("pack(U): value out of range");
    }

    @Specialization(guards = "value > 0x7fffffff")
    public Object writeOutOfRange(long value) {
        throw new RangeException("pack(U): value out of range");
    }

}
