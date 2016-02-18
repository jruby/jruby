/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

public class ArrayIsAtLeastAsLargeAsNode extends RubyNode {

    @Child private RubyNode child;

    private final int requiredSize;

    public ArrayIsAtLeastAsLargeAsNode(RubyContext context, SourceSection sourceSection, RubyNode child, int requiredSize) {
        super(context, sourceSection);
        this.child = child;
        this.requiredSize = requiredSize;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        return Layouts.ARRAY.getSize((DynamicObject) child.execute(frame)) >= requiredSize;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
