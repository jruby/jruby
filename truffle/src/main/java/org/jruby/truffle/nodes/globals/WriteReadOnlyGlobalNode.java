/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.globals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

public class WriteReadOnlyGlobalNode extends RubyNode {

    private final String name;
    @Child private RubyNode value;

    public WriteReadOnlyGlobalNode(RubyContext context, SourceSection sourceSection, String name, RubyNode value) {
        super(context, sourceSection);
        this.name = name;
        this.value = value;
    }

    public void executeVoid(VirtualFrame frame) {
        value.executeVoid(frame);
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().nameErrorReadOnly(name, this));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return nil();
    }

}
