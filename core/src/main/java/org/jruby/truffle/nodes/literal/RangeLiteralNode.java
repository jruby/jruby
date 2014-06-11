/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyRange;

@NodeInfo(shortName = "range")
@NodeChildren({@NodeChild("begin"), @NodeChild("end")})
public abstract class RangeLiteralNode extends RubyNode {

    private final boolean excludeEnd;

    public RangeLiteralNode(RubyContext context, SourceSection sourceSection, boolean excludeEnd) {
        super(context, sourceSection);
        this.excludeEnd = excludeEnd;
    }

    public RangeLiteralNode(RangeLiteralNode prev) {
        this(prev.getContext(), prev.getSourceSection(), prev.excludeEnd);
    }

    @Specialization
    public RubyRange.IntegerFixnumRange doFixnum(int begin, int end) {
        return new RubyRange.IntegerFixnumRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

    @Specialization
    public Object doObject(Object begin, Object end) {
        return new RubyRange.ObjectRange(getContext().getCoreLibrary().getRangeClass(), begin, end, excludeEnd);
    }

}
