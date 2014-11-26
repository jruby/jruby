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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.KillException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;

import java.math.BigInteger;

public abstract class WrapperNode extends RubyNode {

    @Child protected RubyNode child;

    public WrapperNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        assert !(child instanceof WrapperNode);
        this.child = child;
    }

    abstract void enter(VirtualFrame frame);
    abstract void leave(VirtualFrame frame);
    abstract void leave(VirtualFrame frame, boolean result);
    abstract void leave(VirtualFrame frame, int result);
    abstract void leave(VirtualFrame frame, long result);
    abstract void leave(VirtualFrame frame, double result);
    abstract void leave(VirtualFrame frame, Object result);
    abstract void leaveExceptional(VirtualFrame frame, Exception e);

    @Override
    public Object execute(VirtualFrame frame) {
        enter(frame);

        final Object result;

        try {
            result = child.execute(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyArray executeArray(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyArray result;

        try {
            result = child.executeArray(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public BigInteger executeBignum(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final BigInteger result;

        try {
            result = child.executeBignum(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final boolean result;

        try {
            result = child.executeBoolean(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public int executeIntegerFixnum(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final int result;

        try {
            result = child.executeIntegerFixnum(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public long executeLongFixnum(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final long result;

        try {
            result = child.executeLongFixnum(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRange.IntegerFixnumRange executeIntegerFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyRange.IntegerFixnumRange result;

        try {
            result = child.executeIntegerFixnumRange(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public double executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final double result;

        try {
            result = child.executeFloat(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public Object[] executeObjectArray(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final Object[] result;

        try {
            result = child.executeObjectArray(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRange.ObjectRange executeObjectRange(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyRange.ObjectRange result;

        try {
            result = child.executeObjectRange(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyBasicObject result;

        try {
            result = child.executeRubyBasicObject(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyBinding executeRubyBinding(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyBinding result;

        try {
            result = child.executeRubyBinding(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyClass executeRubyClass(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyClass result;

        try {
            result = child.executeRubyClass(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyContinuation executeRubyContinuation(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyContinuation result;

        try {
            result = child.executeRubyContinuation(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyException executeRubyException(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyException result;

        try {
            result = child.executeRubyException(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyFiber executeRubyFiber(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyFiber result;

        try {
            result = child.executeRubyFiber(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyFile executeRubyFile(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyFile result;

        try {
            result = child.executeRubyFile(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyHash executeRubyHash(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyHash result;

        try {
            result = child.executeRubyHash(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyMatchData executeRubyMatchData(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyMatchData result;

        try {
            result = child.executeRubyMatchData(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyModule executeRubyModule(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyModule result;

        try {
            result = child.executeRubyModule(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyNilClass executeRubyNilClass(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyNilClass result;

        try {
            result = child.executeRubyNilClass(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyObject executeRubyObject(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyObject result;

        try {
            result = child.executeRubyObject(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyProc executeRubyProc(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyProc result;

        try {
            result = child.executeRubyProc(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRange executeRubyRange(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyRange result;

        try {
            result = child.executeRubyRange(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyRegexp executeRubyRegexp(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyRegexp result;

        try {
            result = child.executeRubyRegexp(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubySymbol executeRubySymbol(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubySymbol result;

        try {
            result = child.executeRubySymbol(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyThread executeRubyThread(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyThread result;

        try {
            result = child.executeRubyThread(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyTime executeRubyTime(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyTime result;

        try {
            result = child.executeRubyTime(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyString executeString(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyString result;

        try {
            result = child.executeString(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public RubyEncoding executeRubyEncoding(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final RubyEncoding result;

        try {
            result = child.executeRubyEncoding(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public UndefinedPlaceholder executeUndefinedPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        enter(frame);

        final UndefinedPlaceholder result;

        try {
            result = child.executeUndefinedPlaceholder(frame);
            leave(frame, result);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }

        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        enter(frame);

        try {
            child.executeVoid(frame);
            leave(frame);
        } catch (KillException e) {
            throw (e);
        } catch (Exception e) {
            leaveExceptional(frame, e);
            throw e;
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return child.isDefined(frame);
    }

}
