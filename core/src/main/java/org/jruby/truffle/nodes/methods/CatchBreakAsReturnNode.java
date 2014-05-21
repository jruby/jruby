/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;

/**
 * Catch a {@code break} jump in a block at the top level and handle it as a return.
 */
public class CatchBreakAsReturnNode extends RubyNode {

    @Child protected RubyNode body;

    private final BranchProfile nextProfile = new BranchProfile();

    public CatchBreakAsReturnNode(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        try {
            return body.execute(frame);
        } catch (BreakException e) {
            nextProfile.enter();
            return e.getResult();
        }
    }

}
