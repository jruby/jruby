/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.language.RubyNode;

@NodeInfo(cost = NodeCost.NONE)
public class NilLiteralNode extends RubyNode {

    private final boolean isImplicit;

    public NilLiteralNode(boolean isImplicit) {
        this.isImplicit = isImplicit;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return nil();
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().NIL.createInstance();
    }

    public boolean isImplicit() {
        return isImplicit;
    }

}
