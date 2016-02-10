/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.instrument;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.EventHandlerNode;
import com.oracle.truffle.api.instrument.KillException;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.WrapperNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.language.RubyNode;

@NodeInfo(cost = NodeCost.NONE)
public final class RubyWrapperNode extends RubyNode implements WrapperNode {

    @Child private RubyNode child;
    @Child private EventHandlerNode eventHandlerNode;

    public RubyWrapperNode(RubyNode child) {
        super(child.getContext(), child.getSourceSection());
        assert !(child instanceof RubyWrapperNode);
        this.child = child;
    }

    public String instrumentationInfo() {
        return "Wrapper node for Ruby";
    }

    public void insertEventHandlerNode(EventHandlerNode newProbeNode) {
        this.eventHandlerNode = insert(newProbeNode);
    }

    public Probe getProbe() {
        try {
            return eventHandlerNode.getProbe();
        } catch (IllegalStateException e) {
            throw new UnsupportedOperationException("A lite-Probed wrapper has no explicit Probe");
        }
    }

    public RubyNode getChild() {
        return child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        eventHandlerNode.enter(child, frame);

        Object result;

        try {
            result = child.execute(frame);
            eventHandlerNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        eventHandlerNode.enter(child, frame);

        boolean result;

        try {
            result = child.executeBoolean(frame);
            eventHandlerNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return child.isDefined(frame);
    }

    @Override
    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        eventHandlerNode.enter(child, frame);

        int result;

        try {
            result = child.executeInteger(frame);
            eventHandlerNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        eventHandlerNode.enter(child, frame);

        long result;

        try {
            result = child.executeLong(frame);
            eventHandlerNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        eventHandlerNode.enter(child, frame);

        double result;

        try {
            result = child.executeDouble(frame);
            eventHandlerNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        eventHandlerNode.enter(child, frame);

        try {
            child.executeVoid(frame);
            eventHandlerNode.returnVoid(child, frame);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            eventHandlerNode.returnExceptional(child, frame, e);
            throw e;
        }
    }

}
