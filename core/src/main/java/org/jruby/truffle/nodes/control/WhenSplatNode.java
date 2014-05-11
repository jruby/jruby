/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.call.DispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.array.RubyArray;

@NodeInfo(shortName = "when*")
public class WhenSplatNode extends RubyNode {

    @Child protected RubyNode readCaseExpression;
    @Child protected RubyNode splat;
    @Child protected DispatchHeadNode dispatchThreeEqual;

    public WhenSplatNode(RubyContext context, SourceSection sourceSection, RubyNode readCaseExpression, RubyNode splat) {
        super(context, sourceSection);
        this.readCaseExpression = readCaseExpression;
        this.splat = splat;
        dispatchThreeEqual = new DispatchHeadNode(context, "===", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        final Object caseExpression = readCaseExpression.execute(frame);

        final RubyArray array;

        try {
            array = splat.executeArray(frame);
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(e);
        }

        for (Object value : array.asList()) {
            // TODO(CS): how to cast this to a boolean?

            if ((boolean) dispatchThreeEqual.dispatch(frame, caseExpression, null, value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
