/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;

public class ReturnNode extends RubyNode {

    private final ReturnID returnID;

    @Child private RubyNode value;

    public ReturnNode(ReturnID returnID, RubyNode value) {
        this.returnID = returnID;
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new ReturnException(returnID, value.execute(frame));
    }

}
