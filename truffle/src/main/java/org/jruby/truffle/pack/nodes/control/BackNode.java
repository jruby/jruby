/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.runtime.OutsideOfStringException;

public class BackNode extends PackNode {

    @Override
    public Object execute(VirtualFrame frame) {
        final int position = getOutputPosition(frame);

        if (position == 0) {
            CompilerDirectives.transferToInterpreter();
            throw new OutsideOfStringException();
        }

        setOutputPosition(frame, position - 1);

        return null;
    }

}
