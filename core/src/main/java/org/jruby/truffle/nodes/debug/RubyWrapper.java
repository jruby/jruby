/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.KillException;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.SyntaxTag;
import com.oracle.truffle.api.instrument.Wrapper;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;

import java.math.BigInteger;

public class RubyWrapper extends ProxyNode implements Wrapper {

    @Child protected Probe probe;

    public RubyWrapper(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection, child);
        assert !(child instanceof RubyWrapper);
        probe = context.createProbe(child.getSourceSection());
    }

    @Override
    void enter(VirtualFrame frame) {
        probe.enter(getChild(), frame);
    }

    @Override
    void leave(VirtualFrame frame) {
        probe.leave(getChild(), frame);
    }

    @Override
    void leave(VirtualFrame frame, boolean result) {
        probe.leave(getChild(), frame, result);
    }

    @Override
    void leave(VirtualFrame frame, int result) {
        probe.leave(getChild(), frame, result);
    }

    @Override
    void leave(VirtualFrame frame, long result) {
        probe.leave(getChild(), frame, result);
    }

    @Override
    void leave(VirtualFrame frame, double result) {
        probe.leave(getChild(), frame, result);
    }

    @Override
    void leave(VirtualFrame frame, Object result) {
        probe.leave(getChild(), frame);
    }

    @Override
    void leaveExceptional(VirtualFrame frame, Exception e) {
        probe.leaveExceptional(getChild(), frame, e);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return child.isDefined(frame);
    }

    @Override
    public Probe getProbe() {
        return probe;
    }

    public void tagAs(SyntaxTag tag) {
        probe.tagAs(tag);
    }

}
