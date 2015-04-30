/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes;

import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.pack.runtime.PackFrameDescriptor;

/**
 * Reads the source array from the frame - written as a node so that we can
 * write DSL specializations based on the type of that array.
 */
public class SourceNode extends Node {

    public Object execute(VirtualFrame frame) {
        try {
            return frame.getObject(PackFrameDescriptor.SOURCE_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

}
