/*
 * Copyright (c) 2015 Software Architecture Group, Hasso Plattner Institute.
 * All rights reserved. This code is released under a tri EPL/GPL/LGPL license.
 * You can use it, redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public class OptionalKeywordArgMissingNode extends RubyNode {

    private static OptionalKeywordArgMissing instance = new OptionalKeywordArgMissing();

    public OptionalKeywordArgMissingNode(RubyContext context,
            SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public static class OptionalKeywordArgMissing {}

    @Override
    public Object execute(VirtualFrame frame) {
        return instance;
    }

}
