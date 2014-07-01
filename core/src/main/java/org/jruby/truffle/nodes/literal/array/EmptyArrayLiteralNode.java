/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal.array;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;

public class EmptyArrayLiteralNode extends ArrayLiteralNode {

    public EmptyArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
        super(context, sourceSection, values);
    }

    @Override
    public RubyArray executeArray(VirtualFrame frame) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
    }

}
