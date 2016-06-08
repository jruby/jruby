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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.AssumedValue;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

/**
 * Executes a child node just once, and uses the same value each subsequent time the node is exeuted.
 */
public class OnceNode extends RubyNode {

    @Child private RubyNode child;

    private final AssumedValue<Object> valueMemo = new AssumedValue<>(OnceNode.class.getName(), null);

    public OnceNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = valueMemo.get();

        if (value == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            value = child.execute(frame);
            valueMemo.set(value);
        }

        return value;
    }

}
