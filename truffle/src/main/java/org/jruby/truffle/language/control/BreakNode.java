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
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;

public class BreakNode extends RubyNode {

    private final BreakID breakID;
    private final boolean ignoreMarker;

    @Child private RubyNode child;

    private final BranchProfile breakFromProcClosureProfile = BranchProfile.create();

    public BreakNode(BreakID breakID, boolean ignoreMarker, RubyNode child) {
        this.breakID = breakID;
        this.ignoreMarker = ignoreMarker;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (!ignoreMarker) {
            final FrameOnStackMarker marker = RubyArguments.getFrameOnStackMarker(frame);

            if (marker != null && !marker.isOnStack()) {
                breakFromProcClosureProfile.enter();
                throw new RaiseException(coreExceptions().breakFromProcClosure(this));
            }
        }

        throw new BreakException(breakID, child.execute(frame));
    }

}
