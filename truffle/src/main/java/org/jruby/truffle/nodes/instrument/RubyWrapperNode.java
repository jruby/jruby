/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.instrument;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.KillException;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyString;

@NodeInfo(cost = NodeCost.NONE)
public final class RubyWrapperNode extends RubyNode implements WrapperNode {

    @Child private RubyNode child;
    @Child private ProbeNode probeNode;

    public RubyWrapperNode(RubyNode child) {
        super(child.getContext(), child.getSourceSection());
        assert !(child instanceof RubyWrapperNode);
        this.child = child;
    }

    public String instrumentationInfo() {
        return "Wrapper node for Ruby";
    }

    public void insertProbe(ProbeNode newProbeNode) {
        this.probeNode = insert(newProbeNode);
    }

    public Probe getProbe() {
        try {
            return probeNode.getProbe();
        } catch (IllegalStateException e) {
            throw new UnsupportedOperationException("A lite-Probed wrapper has no explicit Probe");
        }
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    public RubyNode getChild() {
        return child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        probeNode.enter(child, frame);

        Object result;

        try {
            result = child.execute(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyArray executeRubyArray(VirtualFrame frame) throws UnexpectedResultException {
        probeNode.enter(child, frame);

        RubyArray result;

        try {
            result = child.executeRubyArray(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyBignum executeRubyBignum(VirtualFrame frame) throws UnexpectedResultException {
        probeNode.enter(child, frame);

        RubyBignum result;

        try {
            result = child.executeRubyBignum(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        probeNode.enter(child, frame);

        boolean result;

        try {
            result = child.executeBoolean(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
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
        probeNode.enter(child, frame);

        int result;

        try {
            result = child.executeInteger(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        probeNode.enter(child, frame);

        long result;

        try {
            result = child.executeLong(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        probeNode.enter(child, frame);

        double result;

        try {
            result = child.executeDouble(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyString executeRubyString(VirtualFrame frame) throws UnexpectedResultException {
        probeNode.enter(child, frame);

        RubyString result;

        try {
            result = child.executeRubyString(frame);
            probeNode.returnValue(child, frame, result);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        probeNode.enter(child, frame);

        try {
            child.executeVoid(frame);
            probeNode.returnVoid(child, frame);
        } catch (KillException e) {
            throw e;
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw e;
        }
    }

}
