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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;

/**
 * Executes a child node just once, and uses the same value each subsequent time the node is
 * executed.
 */
public class OnceNode extends RubyNode {

    @Child private RubyNode child;

    @CompilationFinal private volatile Object cachedValue;

    public OnceNode(RubyNode child) {
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = cachedValue;

        if (value == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            synchronized (this) {
                if (cachedValue == null) {
                    value = cachedValue = child.execute(frame);
                    assert value != null;
                }
            }
        }

        return value;
    }

}
