/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.parser.jruby;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

/**
 * Dead nodes are removed wherever they are found during translation. They fill in for some missing
 * nodes when we're processing the AST.
 */
public class DeadNode extends RubyNode {

    private final Exception reason;

    public DeadNode(RubyContext context, SourceSection sourceSection, Exception reason) {
        super(context, sourceSection);
        this.reason = reason;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw exception();
    }

    @TruffleBoundary
    private RuntimeException exception() {
        return new UnsupportedOperationException(reason);
    }
}
