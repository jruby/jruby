/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.core.BindingNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

@Deprecated
public class RubyBinding extends RubyBasicObject {

    @CompilationFinal public Object self;
    @CompilationFinal public MaterializedFrame frame;

    public RubyBinding(RubyBasicObject bindingClass, Object self, MaterializedFrame frame) {
        super(bindingClass);
        this.self = self;
        this.frame = frame;
    }

    public static class BindingAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            return BindingNodes.createRubyBinding(rubyClass);
        }
    }

}
