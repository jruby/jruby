/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.nodes.PackNode;
import org.jruby.truffle.core.format.runtime.exceptions.OutsideOfStringException;

public class AtUnpackNode extends PackNode {

    private final int position;

    public AtUnpackNode(RubyContext context, int position) {
        super(context);
        this.position = position;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (position > getSourceLength(frame)) {
            throw new OutsideOfStringException();
        }

        setSourcePosition(frame, position);

        return null;
    }

}
