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
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyRange;

@NodeChildren({@NodeChild("begin"), @NodeChild("end")})
public abstract class RangeLiteralNode extends RubyNode {

    private final boolean excludeEnd;

    private final BranchProfile beginIntegerProfile = BranchProfile.create();
    private final BranchProfile beginLongProfile = BranchProfile.create();
    private final BranchProfile endIntegerProfile = BranchProfile.create();
    private final BranchProfile endLongProfile = BranchProfile.create();
    private final BranchProfile objectProfile = BranchProfile.create();

    @Child private CallDispatchHeadNode cmpNode;

    public RangeLiteralNode(RubyContext context, SourceSection sourceSection, boolean excludeEnd) {
        super(context, sourceSection);
        this.excludeEnd = excludeEnd;
    }

    public RangeLiteralNode(RangeLiteralNode prev) {
        this(prev.getContext(), prev.getSourceSection(), prev.excludeEnd);
    }

    @Specialization
    public RubyRange.IntegerFixnumRange doRange(int begin, int end) {
        return new RubyRange.IntegerFixnumRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    @Specialization
    public RubyRange.LongFixnumRange doRange(int begin, long end) {
        return new RubyRange.LongFixnumRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    @Specialization
    public RubyRange.LongFixnumRange doRange(long begin, int end) {
        return new RubyRange.LongFixnumRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    @Specialization
    public RubyRange.LongFixnumRange doRange(long begin, long end) {
        return new RubyRange.LongFixnumRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    @Specialization
    public Object doRange(VirtualFrame frame, Object begin, Object end) {
        if (begin instanceof Integer) {
            beginIntegerProfile.enter();

            if (end instanceof Integer) {
                endIntegerProfile.enter();
                return doRange((int) begin, (int) end);
            }

            if (end instanceof Long) {
                endLongProfile.enter();
                return doRange((int) begin, (long) end);
            }
        } else if (begin instanceof Long) {
            beginLongProfile.enter();

            if (end instanceof Integer) {
                endIntegerProfile.enter();
                return doRange((long) begin, (int) end);
            }

            if (end instanceof Long) {
                endLongProfile.enter();
                return doRange((long) begin, (long) end);
            }
        }

        objectProfile.enter();

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

        if (cmpResult == getContext().getCoreLibrary().getNilObject()) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("bad value for range", this));
        }

        return new RubyRange.ObjectRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

}
