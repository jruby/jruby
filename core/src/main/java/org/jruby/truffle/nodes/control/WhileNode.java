/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;

public class WhileNode extends RubyNode {

    @Child protected LoopNode loopNode;

    public WhileNode(RubyContext context, SourceSection sourceSection, BooleanCastNode condition, RubyNode body) {
        super(context, sourceSection);
        loopNode = Truffle.getRuntime().createLoopNode(new WhileRepeatingNode(condition, body));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            loopNode.executeLoop(frame);
        } catch (BreakException e) {
            return e.getResult();
        }

        return getContext().getCoreLibrary().getNilObject();
    }

    public class WhileRepeatingNode extends Node implements RepeatingNode {

        @Child protected BooleanCastNode condition;
        @Child protected RubyNode body;

        public WhileRepeatingNode(BooleanCastNode condition, RubyNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (!condition.executeBoolean(frame)) {
                return false;
            }

            while (true) {
                try {
                    body.execute(frame);
                    return true;
                } catch (NextException e) {
                    return true;
                } catch (RedoException e) {
                }
            }
        }

    }

}
