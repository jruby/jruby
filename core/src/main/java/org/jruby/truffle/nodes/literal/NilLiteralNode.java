/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;

/**
 * A node that does nothing and evaluates to {@code nil}. Also a no-op.
 */
public final class NilLiteralNode extends RubyNode {

    public NilLiteralNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public NilPlaceholder executeNilPlaceholder(VirtualFrame frame) {
        return NilPlaceholder.INSTANCE;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeNilPlaceholder(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return NilPlaceholder.INSTANCE;
    }

}
