/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.time;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;

public class ReadTimeZoneNode extends RubyNode {
    
    @Child private CallDispatchHeadNode hashNode;
    
    private final RubyString TZ;
    
    public ReadTimeZoneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        hashNode = DispatchHeadNodeFactory.createMethodCall(context);
        TZ = getContext().makeString("TZ");
    }

    @Override
    public RubyString executeRubyString(VirtualFrame frame) {
        final Object tz = hashNode.call(frame, getContext().getCoreLibrary().getENV(), "[]", null, TZ);

        // TODO CS 4-May-15 not sure how TZ ends up being nil

        if (tz == nil()) {
            return getContext().makeString("UTC");
        } else if (tz instanceof RubyString) {
            return (RubyString) tz;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyString(frame);
    }
}
