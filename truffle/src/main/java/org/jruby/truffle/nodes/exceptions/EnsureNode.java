/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Represents an ensure clause in exception handling. Represented separately to the try part.
 */
public class EnsureNode extends RubyNode {

    @Child private RubyNode tryPart;
    @Child private RubyNode ensurePart;

    public EnsureNode(RubyContext context, SourceSection sourceSection, RubyNode tryPart, RubyNode ensurePart) {
        super(context, sourceSection);
        this.tryPart = tryPart;
        this.ensurePart = ensurePart;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return tryPart.execute(frame);
        } finally {
            ensurePart.executeVoid(frame);
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            tryPart.executeVoid(frame);
        } finally {
            ensurePart.executeVoid(frame);
        }
    }

}
