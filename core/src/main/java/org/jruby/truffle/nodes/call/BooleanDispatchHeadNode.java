/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class BooleanDispatchHeadNode extends Node {

    @Child protected DispatchHeadNode dispatch;
    @Child protected BooleanCastNode booleanCast;

    public BooleanDispatchHeadNode(RubyContext context, SourceSection sourceSection, DispatchHeadNode dispatch) {
        this.dispatch = dispatch;
        booleanCast = BooleanCastNodeFactory.create(context, sourceSection, null);
    }

    public boolean executeBoolean(VirtualFrame frame, Object receiverObject, RubyProc blockObject, Object... args) {
        return booleanCast.executeBoolean(frame, dispatch.dispatch(frame, receiverObject, blockObject, args));
    }

    public boolean executeBoolean(VirtualFrame frame, Object callingSelf, Object receiverObject, RubyProc blockObject, Object... args) {
        return booleanCast.executeBoolean(frame, dispatch.dispatch(frame, callingSelf, receiverObject, blockObject, args));
    }

}
