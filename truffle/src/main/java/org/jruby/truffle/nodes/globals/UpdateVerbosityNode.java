/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.globals;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class UpdateVerbosityNode extends RubyNode {

    @Child private RubyNode child;

    public UpdateVerbosityNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation("f5e853e9a6cd4d17a405ce1775cbe7da");

        final Object childValue = child.execute(frame);

        final IRubyObject jrubyValue = getContext().toJRuby(childValue);

        getContext().getRuntime().setVerbose(jrubyValue);

        return childValue;
    }

}
