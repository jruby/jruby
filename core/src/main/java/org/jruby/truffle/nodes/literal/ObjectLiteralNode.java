/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import java.math.*;

import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@NodeInfo(cost = NodeCost.NONE)
public class ObjectLiteralNode extends RubyNode {

    private final Object object;

    public ObjectLiteralNode(RubyContext context, SourceSection sourceSection, Object object) {
        super(context, sourceSection);
        this.object = object;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return object;
    }

    public Object getObject() {
        return object;
    }
}
