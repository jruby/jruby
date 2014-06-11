/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Represents a Ruby {@code and} or {@code &&} expression.
 */
@NodeInfo(shortName = "and")
public class AndNode extends RubyNode {

    @Child protected RubyNode left;
    @Child protected BooleanCastNode leftCast;
    @Child protected RubyNode right;

    public AndNode(RubyContext context, SourceSection sourceSection, RubyNode left, RubyNode right) {
        super(context, sourceSection);
        this.left = left;
        leftCast = BooleanCastNodeFactory.create(context, sourceSection, null);
        this.right = right;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object leftValue = left.execute(frame);

        if (!leftCast.executeBoolean(frame, leftValue)) {
            return leftValue;
        }

        return right.execute(frame);
    }

}
