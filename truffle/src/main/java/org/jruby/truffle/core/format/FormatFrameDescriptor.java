/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public class FormatFrameDescriptor {

    public static final FrameDescriptor FRAME_DESCRIPTOR = new FrameDescriptor();
    public static final FrameSlot SOURCE_SLOT = FRAME_DESCRIPTOR.addFrameSlot("source", FrameSlotKind.Object);
    public static final FrameSlot SOURCE_LENGTH_SLOT = FRAME_DESCRIPTOR.addFrameSlot("source-length", FrameSlotKind.Int);
    public static final FrameSlot SOURCE_POSITION_SLOT = FRAME_DESCRIPTOR.addFrameSlot("source-position", FrameSlotKind.Int);
    public static final FrameSlot OUTPUT_SLOT = FRAME_DESCRIPTOR.addFrameSlot("output", FrameSlotKind.Object);
    public static final FrameSlot OUTPUT_POSITION_SLOT = FRAME_DESCRIPTOR.addFrameSlot("output-position", FrameSlotKind.Int);
    public static final FrameSlot STRING_LENGTH_SLOT = FRAME_DESCRIPTOR.addFrameSlot("string-length", FrameSlotKind.Int);
    public static final FrameSlot STRING_CODE_RANGE_SLOT = FRAME_DESCRIPTOR.addFrameSlot("string-code-range", FrameSlotKind.Int);
    public static final FrameSlot TAINT_SLOT = FRAME_DESCRIPTOR.addFrameSlot("taint", FrameSlotKind.Boolean);

}
