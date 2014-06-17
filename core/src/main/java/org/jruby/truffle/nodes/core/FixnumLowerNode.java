/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.control.PassthroughNode;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyFixnum;
import org.jruby.truffle.runtime.core.RubyRange;

public class FixnumLowerNode extends PassthroughNode {

    @CompilerDirectives.CompilationFinal private boolean hasNeededToLowerLongFixnum = false;
    @CompilerDirectives.CompilationFinal private boolean hasNeededToLowerLongFixnumRange = false;

    public FixnumLowerNode(RubyNode child) {
        super(child);
    }

    @Override
    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) throws UnexpectedResultException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final Object value = child.execute(frame);

        if (value instanceof Long && canLower((long) value)) {
            hasNeededToLowerLongFixnum = true;
            return lower((long) value);
        }

        if (value instanceof Long) {
            throw new UnsupportedOperationException();
        }

        if (value instanceof RubyRange.LongFixnumRange && canLower((RubyRange.LongFixnumRange) value)) {
            hasNeededToLowerLongFixnumRange = true;
            return lower((RubyRange.LongFixnumRange) value);
        }

        if (value instanceof RubyRange.LongFixnumRange) {
            throw new UnsupportedOperationException();
        }

        return value;
    }

    @Override
    public int executeIntegerFixnum(VirtualFrame frame) throws UnexpectedResultException {
        try {
            if (hasNeededToLowerLongFixnum) {
                final long value = super.executeLongFixnum(frame);

                if (canLower(value)) {
                    return lower(value);
                } else {
                    throw new UnexpectedResultException(value);
                }
            } else {
                return super.executeIntegerFixnum(frame);
            }
        } catch (UnexpectedResultException e) {
            if (e.getResult() instanceof Long && canLower((long) e.getResult())) {
                hasNeededToLowerLongFixnum = true;
                return lower((long) e.getResult());
            } else if (e.getResult() instanceof RubyRange.LongFixnumRange && canLower((RubyRange.LongFixnumRange) e.getResult())) {
                hasNeededToLowerLongFixnumRange = true;
                throw new UnexpectedResultException(lower((RubyRange.LongFixnumRange) e.getResult()));
            } else {
                throw e;
            }
        }
    }

    @Override
    public long executeLongFixnum(VirtualFrame frame) throws UnexpectedResultException {
        throw new RuntimeException();
    }

    @Override
    public RubyRange.IntegerFixnumRange executeIntegerFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        try {
            if (hasNeededToLowerLongFixnumRange) {
                final RubyRange.LongFixnumRange range = super.executeLongFixnumRange(frame);

                if (canLower(range)) {
                    return lower(range);
                } else {
                    throw new UnexpectedResultException(range);
                }
            } else {
                return super.executeIntegerFixnumRange(frame);
            }
        } catch (UnexpectedResultException e) {
            if (e.getResult() instanceof Long && canLower((long) e.getResult())) {
                hasNeededToLowerLongFixnum = true;
                throw new UnexpectedResultException(lower((long) e.getResult()));
            } else if (e.getResult() instanceof RubyRange.LongFixnumRange && canLower((RubyRange.LongFixnumRange) e.getResult())) {
                hasNeededToLowerLongFixnumRange = true;
                return lower((RubyRange.LongFixnumRange) e.getResult());
            } else {
                throw e;
            }
        }
    }

    @Override
    public RubyRange.LongFixnumRange executeLongFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        throw new RuntimeException();

    }

    @Override
    public UndefinedPlaceholder executeUndefinedPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return super.executeUndefinedPlaceholder(frame);
        } catch (UnexpectedResultException e) {
            if (e.getResult() instanceof Long && canLower((long) e.getResult())) {
                hasNeededToLowerLongFixnum = true;
                throw new UnexpectedResultException(lower((long) e.getResult()));
            } else if (e.getResult() instanceof RubyRange.LongFixnumRange && canLower((RubyRange.LongFixnumRange) e.getResult())) {
                hasNeededToLowerLongFixnumRange = true;
                throw new UnexpectedResultException(e.getResult());
            } else {
                throw e;
            }
        }
    }

    private static boolean canLower(long value) {
        return RubyFixnum.fitsIntoInteger(value);
    }

    private static int lower(long value) {
        assert canLower(value);
        return (int) value;
    }

    private static boolean canLower(RubyRange.LongFixnumRange range) {
        return canLower(range.getBegin()) && canLower(range.getEnd());
    }

    private static RubyRange.IntegerFixnumRange lower(RubyRange.LongFixnumRange range) {
        assert canLower(range);
        return new RubyRange.IntegerFixnumRange(range.getRubyClass().getContext().getCoreLibrary().getRangeClass(), lower(range.getBegin()), lower(range.getEnd()), range.doesExcludeEnd());
    }

}
