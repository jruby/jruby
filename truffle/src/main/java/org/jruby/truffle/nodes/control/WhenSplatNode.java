/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;

public class WhenSplatNode extends RubyNode {

    @Child private RubyNode readCaseExpression;
    @Child private RubyNode splat;
    @Child private CallDispatchHeadNode dispatchCaseEqual;

    public WhenSplatNode(RubyContext context, SourceSection sourceSection, RubyNode readCaseExpression, RubyNode splat) {
        super(context, sourceSection);
        this.readCaseExpression = readCaseExpression;
        this.splat = splat;
        dispatchCaseEqual = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final Object caseExpression = readCaseExpression.execute(frame);

        final RubyArray array;

        try {
            array = splat.executeRubyArray(frame);
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(e);
        }

        for (Object value : array.slowToArray()) {
            if (dispatchCaseEqual.callBoolean(frame, caseExpression, "===", null, value)) {
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
