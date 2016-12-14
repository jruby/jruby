/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.language.RubyNode;

/**
 * This node has a pair of children. One has side effects and the other returns the
 * result. If the result isn't needed all we execute is the side effects.
 */
@NodeInfo(cost = NodeCost.NONE)
public class ElidableResultNode extends RubyNode {

    @Child private RubyNode required;
    @Child private RubyNode elidableResult;

    public ElidableResultNode(RubyNode required, RubyNode elidableResult) {
        this.required = required;
        this.elidableResult = elidableResult;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        required.executeVoid(frame);
        return elidableResult.execute(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        required.execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return elidableResult.isDefined(frame);
    }

}
