/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.CoreLibrary;
import org.jruby.truffle.runtime.core.RubyRange;

@NodeChildren({@NodeChild("begin"), @NodeChild("end")})
public abstract class RangeLiteralNode extends RubyNode {

    private final boolean excludeEnd;

    @Child private CallDispatchHeadNode cmpNode;

    public RangeLiteralNode(RubyContext context, SourceSection sourceSection, boolean excludeEnd) {
        super(context, sourceSection);
        this.excludeEnd = excludeEnd;
    }

    @Specialization
    public RubyRange.IntegerFixnumRange intRange(int begin, int end) {
        return new RubyRange.IntegerFixnumRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    @Specialization(guards = { "fitsIntoInteger(begin)", "fitsIntoInteger(end)" })
    public RubyRange.IntegerFixnumRange longFittingIntRange(long begin, long end) {
        return new RubyRange.IntegerFixnumRange(getContext().getCoreLibrary().getRangeClass(), (int) begin, (int) end, excludeEnd);
    }

    @Specialization(guards = "!fitsIntoInteger(begin) || !fitsIntoInteger(end)")
    public RubyRange.LongFixnumRange longRange(long begin, long end) {
        return new RubyRange.LongFixnumRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    @Specialization(guards = { "!isIntOrLong(begin) || !isIntOrLong(end)" })
    public Object doRange(VirtualFrame frame, Object begin, Object end) {
        if (cmpNode == null) {
            CompilerDirectives.transferToInterpreter();
            cmpNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        final Object cmpResult;
        try {
            cmpResult = cmpNode.call(frame, begin, "<=>", null, end);
        } catch (RaiseException e) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("bad value for range", this));
        }

        if (cmpResult == nil()) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("bad value for range", this));
        }

        return new RubyRange.ObjectRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    protected boolean fitsIntoInteger(long value) {
        return CoreLibrary.fitsIntoInteger(value);
    }

    protected boolean isIntOrLong(Object value) {
        return RubyGuards.isInteger(value) || RubyGuards.isLong(value);
    }

}
