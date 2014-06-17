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
import com.oracle.truffle.api.instrument.PhylumTag;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.Wrapper;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;

import java.math.BigInteger;

public class RubyWrapper extends RubyNode implements Wrapper {

    @Child protected RubyNode child;
    @Child protected Probe probe;

    public RubyWrapper(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        assert !(child instanceof RubyWrapper);
        this.child = child;
        probe = context.getProbe(child.getSourceSection());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        probe.enter(child, frame);

        final Object result;

        try {
            result = child.execute(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyArray executeArray(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyArray result;

        try {
            result = child.executeArray(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public BigInteger executeBignum(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final BigInteger result;

        try {
            result = child.executeBignum(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final boolean result;

        try {
            result = child.executeBoolean(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public int executeIntegerFixnum(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final int result;

        try {
            result = child.executeIntegerFixnum(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public long executeLongFixnum(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final long result;

        try {
            result = child.executeLongFixnum(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRange.IntegerFixnumRange executeIntegerFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyRange.IntegerFixnumRange result;

        try {
            result = child.executeIntegerFixnumRange(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public double executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final double result;

        try {
            result = child.executeFloat(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public NilPlaceholder executeNilPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final NilPlaceholder result;

        try {
            result = child.executeNilPlaceholder(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public Object[] executeObjectArray(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final Object[] result;

        try {
            result = child.executeObjectArray(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRange.ObjectRange executeObjectRange(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyRange.ObjectRange result;

        try {
            result = child.executeObjectRange(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyBasicObject result;

        try {
            result = child.executeRubyBasicObject(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyBinding executeRubyBinding(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyBinding result;

        try {
            result = child.executeRubyBinding(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyClass executeRubyClass(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyClass result;

        try {
            result = child.executeRubyClass(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyContinuation executeRubyContinuation(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyContinuation result;

        try {
            result = child.executeRubyContinuation(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyException executeRubyException(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyException result;

        try {
            result = child.executeRubyException(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyFiber executeRubyFiber(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyFiber result;

        try {
            result = child.executeRubyFiber(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyFile executeRubyFile(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyFile result;

        try {
            result = child.executeRubyFile(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyHash executeRubyHash(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyHash result;

        try {
            result = child.executeRubyHash(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyMatchData executeRubyMatchData(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyMatchData result;

        try {
            result = child.executeRubyMatchData(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyModule executeRubyModule(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyModule result;

        try {
            result = child.executeRubyModule(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyNilClass executeRubyNilClass(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyNilClass result;

        try {
            result = child.executeRubyNilClass(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyObject executeRubyObject(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyObject result;

        try {
            result = child.executeRubyObject(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyProc executeRubyProc(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyProc result;

        try {
            result = child.executeRubyProc(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRange executeRubyRange(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyRange result;

        try {
            result = child.executeRubyRange(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRegexp executeRubyRegexp(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyRegexp result;

        try {
            result = child.executeRubyRegexp(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubySymbol executeRubySymbol(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubySymbol result;

        try {
            result = child.executeRubySymbol(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyThread executeRubyThread(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyThread result;

        try {
            result = child.executeRubyThread(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyTime executeRubyTime(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyTime result;

        try {
            result = child.executeRubyTime(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyString executeString(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyString result;

        try {
            result = child.executeString(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyEncoding executeRubyEncoding(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final RubyEncoding result;

        try {
            result = child.executeRubyEncoding(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public UndefinedPlaceholder executeUndefinedPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        probe.enter(child, frame);

        final UndefinedPlaceholder result;

        try {
            result = child.executeUndefinedPlaceholder(frame);
            probe.leave(child, frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        probe.enter(child, frame);

        try {
            child.executeVoid(frame);
            probe.leave(child, frame);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            probe.leaveExceptional(child, frame, e);
            throw e;
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return child.isDefined(frame);
    }

    @Override
    public Node getChild() {
        return child;
    }

    @Override
    public Probe getProbe() {
        return probe;
    }

    @Override
    public boolean isTaggedAs(PhylumTag tag) {
        return probe.isTaggedAs(tag);
    }

    @Override
    public Iterable<PhylumTag> getPhylumTags() {
        return probe.getPhylumTags();
    }

    public void tagAs(PhylumTag tag) {
        probe.tagAs(tag);
    }

}
