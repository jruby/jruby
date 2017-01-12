/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.objects.IsTaintedNode;
import org.jruby.truffle.language.objects.TaintNode;

public class TaintResultNode extends RubyNode {

    private final boolean taintFromSelf;
    private final int taintFromParameter;
    private final ConditionProfile taintProfile = ConditionProfile.createBinaryProfile();

    @Child private RubyNode method;
    @Child private IsTaintedNode isTaintedNode;
    @Child private TaintNode taintNode;

    public TaintResultNode(boolean taintFromSelf, int taintFromParameter, RubyNode method) {
        this.taintFromSelf = taintFromSelf;
        this.taintFromParameter = taintFromParameter;
        this.method = method;
        this.isTaintedNode = IsTaintedNode.create();
    }

    public TaintResultNode() {
        this.taintFromSelf = false;
        this.taintFromParameter = -1;
        this.isTaintedNode = IsTaintedNode.create();
    }

    public Object maybeTaint(Object source, Object result) {
        if (taintProfile.profile(isTaintedNode.executeIsTainted(source))) {
            if (taintNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                taintNode = insert(TaintNode.create());
            }

            taintNode.executeTaint(result);
        }

        return result;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject result;

        try {
            result = method.executeDynamicObject(frame);
        } catch (DoNotTaint e) {
            return e.getResult();
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(e);
        }

        if (result != nil()) {
            if (taintFromSelf) {
                maybeTaint(RubyArguments.getSelf(frame), result);
            }

            if (taintFromParameter != -1) {
                // It's possible the taintFromParameter value was misconfigured by the user, but the far more likely
                // scenario is that the argument at that position is a NotProvided argument, which doesn't take up
                // a space in the frame.
                if (taintFromParameter < RubyArguments.getArgumentsCount(frame)) {
                    final Object argument = RubyArguments.getArgument(frame, taintFromParameter);

                    if (argument instanceof DynamicObject) {
                        final DynamicObject taintSource = (DynamicObject) argument;
                        maybeTaint(taintSource, result);
                    }
                }
            }
        }

        return result;
    }

    public static class DoNotTaint extends ControlFlowException {
        private static final long serialVersionUID = 5321304910918469059L;

        private final Object result;

        public DoNotTaint(Object result) {
            this.result = result;
        }

        public Object getResult() {
            return result;
        }
    }
}
