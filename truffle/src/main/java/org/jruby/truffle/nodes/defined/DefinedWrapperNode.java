/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.defined;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public class DefinedWrapperNode extends RubyNode {

    @Child private RubyNode child;

    private final String definition;

    public DefinedWrapperNode(RubyContext context, SourceSection sourceSection, RubyNode child, String definition) {
        super(context, sourceSection);
        this.child = child;
        this.definition = definition;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return child.execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString(definition);
    }

}
