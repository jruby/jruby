/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Read a post-optional argument.
 */
public class ReadPostArgumentNode extends RubyNode {

    private final int negativeIndex;

    public ReadPostArgumentNode(RubyContext context, SourceSection sourceSection, int negativeIndex) {
        super(context, sourceSection);
        this.negativeIndex = negativeIndex;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int count = RubyArguments.getUserArgumentsCount(frame.getArguments());
        final int effectiveIndex = count + negativeIndex;
        assert effectiveIndex < count;
        return RubyArguments.getUserArgument(frame.getArguments(), effectiveIndex);
    }

}
