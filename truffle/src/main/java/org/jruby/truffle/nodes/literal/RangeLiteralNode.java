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
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.core.RangeNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.CoreLibrary;
import com.oracle.truffle.api.object.DynamicObject;

@NodeChildren({@NodeChild("begin"), @NodeChild("end")})
public abstract class RangeLiteralNode extends RubyNode {

    private final boolean excludeEnd;

    @Child private CallDispatchHeadNode cmpNode;
    @Child private AllocateObjectNode allocateNode;

    public RangeLiteralNode(RubyContext context, SourceSection sourceSection, boolean excludeEnd) {
        super(context, sourceSection);
        this.excludeEnd = excludeEnd;
        allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
    }

    @Specialization
    public DynamicObject intRange(int begin, int end) {
        return RangeNodes.INTEGER_FIXNUM_RANGE_LAYOUT.createIntegerFixnumRange(getContext().getCoreLibrary().getIntegerFixnumRangeFactory(), excludeEnd, begin, end);
    }

    @Specialization(guards = { "fitsIntoInteger(begin)", "fitsIntoInteger(end)" })
    public DynamicObject longFittingIntRange(long begin, long end) {
        return RangeNodes.INTEGER_FIXNUM_RANGE_LAYOUT.createIntegerFixnumRange(getContext().getCoreLibrary().getIntegerFixnumRangeFactory(), excludeEnd, (int) begin, (int) end);
    }

    @Specialization(guards = "!fitsIntoInteger(begin) || !fitsIntoInteger(end)")
    public DynamicObject longRange(long begin, long end) {
        return RangeNodes.LONG_FIXNUM_RANGE_LAYOUT.createLongFixnumRange(getContext().getCoreLibrary().getLongFixnumRangeFactory(), excludeEnd, begin, end);
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

        final DynamicObject rangeClass = getContext().getCoreLibrary().getRangeClass();

        return RangeNodes.OBJECT_RANGE_LAYOUT.createObjectRange(ClassNodes.CLASS_LAYOUT.getInstanceFactory(rangeClass), excludeEnd, begin, end);
    }

    protected boolean fitsIntoInteger(long value) {
        return CoreLibrary.fitsIntoInteger(value);
    }

    protected boolean isIntOrLong(Object value) {
        return RubyGuards.isInteger(value) || RubyGuards.isLong(value);
    }

}
