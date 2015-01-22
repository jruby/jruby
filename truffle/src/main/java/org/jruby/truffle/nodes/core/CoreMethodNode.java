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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value = "arguments", type = RubyNode[].class)
public abstract class CoreMethodNode extends RubyNode {

    @CompilerDirectives.CompilationFinal private String name;

    public CoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public CoreMethodNode(CoreMethodNode prev) {
        super(prev);
        name = prev.name;
    }

    public String getName() {
        if (name == null) {
            throw new UnsupportedOperationException();
        }

        return name;
    }

    public void setName(String name) {
        if (this.name != null) {
            throw new UnsupportedOperationException();
        }

        this.name = name;
    }

}
