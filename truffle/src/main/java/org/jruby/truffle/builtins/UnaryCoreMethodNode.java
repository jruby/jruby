/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.dsl.NodeChild;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SourceIndexLength;

@NodeChild(value = "operand", type = RubyNode.class)
public abstract class UnaryCoreMethodNode extends CoreMethodNode {

    public UnaryCoreMethodNode() {
    }

    public UnaryCoreMethodNode(SourceIndexLength sourceSection) {
        super(sourceSection);
    }

    public abstract RubyNode getOperand();

}
