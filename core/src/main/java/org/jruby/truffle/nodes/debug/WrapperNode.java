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

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;

import java.math.BigInteger;

public abstract class WrapperNode extends RubyNode {

    @Child protected RubyNode child;

    public WrapperNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    protected abstract void before(VirtualFrame frame);

    @Override
    public Object execute(VirtualFrame frame) {
        before(frame);
        return child.execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        before(frame);
        return child.isDefined(frame);
    }

    @Override
    public RubyArray executeArray(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeArray(frame);
    }

    @Override
    public BigInteger executeBignum(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeBignum(frame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeBoolean(frame);
    }

    @Override
    public int executeIntegerFixnum(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeIntegerFixnum(frame);
    }

    @Override
    public long executeLongFixnum(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeLongFixnum(frame);
    }

    @Override
    public RubyRange.IntegerFixnumRange executeFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeFixnumRange(frame);
    }

    @Override
    public double executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeFloat(frame);
    }

    @Override
    public NilPlaceholder executeNilPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeNilPlaceholder(frame);
    }

    @Override
    public Object[] executeObjectArray(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeObjectArray(frame);
    }

    @Override
    public RubyRange.ObjectRange executeObjectRange(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeObjectRange(frame);
    }

    @Override
    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyBasicObject(frame);
    }

    @Override
    public RubyBinding executeRubyBinding(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyBinding(frame);
    }

    @Override
    public RubyClass executeRubyClass(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyClass(frame);
    }

    @Override
    public RubyContinuation executeRubyContinuation(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyContinuation(frame);
    }

    @Override
    public RubyException executeRubyException(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyException(frame);
    }

    @Override
    public RubyFiber executeRubyFiber(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyFiber(frame);
    }

    @Override
    public RubyFile executeRubyFile(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyFile(frame);
    }

    @Override
    public RubyHash executeRubyHash(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyHash(frame);
    }

    @Override
    public RubyMatchData executeRubyMatchData(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyMatchData(frame);
    }

    @Override
    public RubyModule executeRubyModule(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyModule(frame);
    }

    @Override
    public RubyNilClass executeRubyNilClass(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyNilClass(frame);
    }

    @Override
    public RubyObject executeRubyObject(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyObject(frame);
    }

    @Override
    public RubyProc executeRubyProc(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyProc(frame);
    }

    @Override
    public RubyRange executeRubyRange(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyRange(frame);
    }

    @Override
    public RubyRegexp executeRubyRegexp(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyRegexp(frame);
    }

    @Override
    public RubySymbol executeRubySymbol(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubySymbol(frame);
    }

    @Override
    public RubyThread executeRubyThread(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyThread(frame);
    }

    @Override
    public RubyTime executeRubyTime(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyTime(frame);
    }

    @Override
    public RubyString executeString(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeString(frame);
    }

    @Override
    public RubyEncoding executeRubyEncoding(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeRubyEncoding(frame);
    }

    @Override
    public UndefinedPlaceholder executeUndefinedPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        before(frame);
        return child.executeUndefinedPlaceholder(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        before(frame);
        child.executeVoid(frame);
    }

}
