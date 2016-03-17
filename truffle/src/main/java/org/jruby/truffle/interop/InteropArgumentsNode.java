/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

class InteropArgumentsNode extends RubyNode {

    @Children private final InteropArgumentNode[] arguments;

    public InteropArgumentsNode(RubyContext context, SourceSection sourceSection, int arity) {
        super(context, sourceSection);
        this.arguments = new InteropArgumentNode[arity];
        // index 0 is the lable
        for (int i = 1; i < 1 + arity; i++) {
            arguments[i - 1] = new InteropArgumentNode(context, sourceSection, i);
        }
    }

    public int getCount(VirtualFrame frame) {
        return arguments.length;
    }

    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        throw new IllegalStateException();
    }

    @ExplodeLoop
    public void executeFillObjectArray(VirtualFrame frame, Object[] args) {
        for (int i = 0; i < arguments.length; i++) {
            args[i] = arguments[i].execute(frame);
        }
    }
}
