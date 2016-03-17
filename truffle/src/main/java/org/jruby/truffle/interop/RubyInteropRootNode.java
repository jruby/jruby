/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.backtrace.InternalRootNode;

public class RubyInteropRootNode extends RootNode implements InternalRootNode {

    @Child private RubyNode node;

    public RubyInteropRootNode(RubyNode node) {
        super(RubyLanguage.class, node.getSourceSection(), null);
        this.node = node;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        return node.execute(virtualFrame);
    }

    @Override
    public String toString() {
        return "Root of: " + node.toString();
    }
}
