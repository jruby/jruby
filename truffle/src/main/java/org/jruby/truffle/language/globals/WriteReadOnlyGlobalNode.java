/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class WriteReadOnlyGlobalNode extends RubyNode {

    private final String name;

    @Child private RubyNode value;

    public WriteReadOnlyGlobalNode(String name, RubyNode value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        value.executeVoid(frame);
        throw new RaiseException(coreExceptions().nameErrorReadOnly(name, this));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return nil();
    }

}
