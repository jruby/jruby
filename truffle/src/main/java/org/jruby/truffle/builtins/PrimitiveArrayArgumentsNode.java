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

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.PrimitiveNode;
import org.jruby.truffle.language.RubyNode;

@GenerateNodeFactory
@NodeChild(value = "arguments", type = RubyNode[].class)
public abstract class PrimitiveArrayArgumentsNode extends PrimitiveNode {

    public PrimitiveArrayArgumentsNode() {
    }

    public PrimitiveArrayArgumentsNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

}
