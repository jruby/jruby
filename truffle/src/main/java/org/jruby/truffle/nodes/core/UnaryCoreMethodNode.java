/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value = "operand", type = RubyNode.class)
public abstract class UnaryCoreMethodNode extends RubyNode {

    public UnaryCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public UnaryCoreMethodNode(UnaryCoreMethodNode prev) {
        super(prev);
    }

    public abstract RubyNode getOperand();

}
