/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.FrameOnStackMarker;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.translator.TranslatorEnvironment.BreakID;

public class BreakNode extends RubyNode {

    private final BreakID breakID;

    @Child private RubyNode child;
    private final boolean ignoreMarker;

    public BreakNode(RubyContext context, SourceSection sourceSection, BreakID breakID, RubyNode child, boolean ignoreMarker) {
        super(context, sourceSection);
        this.breakID = breakID;
        this.child = child;
        this.ignoreMarker = ignoreMarker;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (!ignoreMarker) {
            final FrameOnStackMarker marker = RubyArguments.getFrameOnStackMarker(frame.getArguments());

            if (marker != null && !marker.isOnStack()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().localJumpError("break from proc-closure", this));
            }
        }

        throw new BreakException(breakID, child.execute(frame));
    }

}
