/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.*;
import jnr.ffi.provider.MemoryManager;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

@GenerateNodeFactory
@NodeChild(value = "arguments", type = RubyNode[].class)
public abstract class RubiniusPrimitiveNode extends RubyNode {

    public RubiniusPrimitiveNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

}
