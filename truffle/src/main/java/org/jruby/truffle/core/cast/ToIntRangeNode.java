/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

@NodeChild("range")
public abstract class ToIntRangeNode extends RubyNode {

    public static ToIntRangeNode create() {
        return ToIntRangeNodeGen.create(null);
    }

    @Child private ToIntNode toIntNode;

    public abstract DynamicObject executeToIntRange(VirtualFrame frame, DynamicObject range);

    @Specialization(guards = "isIntegerFixnumRange(range)")
    public DynamicObject intRange(DynamicObject range) {
        return range;
    }

    @Specialization(guards = "isLongFixnumRange(range)")
    public DynamicObject longRange(VirtualFrame frame, DynamicObject range) {
        int begin = toInt(frame, Layouts.LONG_FIXNUM_RANGE.getBegin(range));
        int end = toInt(frame, Layouts.LONG_FIXNUM_RANGE.getEnd(range));
        boolean excludedEnd = Layouts.LONG_FIXNUM_RANGE.getExcludedEnd(range);
        return Layouts.INTEGER_FIXNUM_RANGE.createIntegerFixnumRange(
                coreLibrary().getIntegerFixnumRangeFactory(), excludedEnd, begin, end);
    }

    @Specialization(guards = "isObjectRange(range)")
    public DynamicObject objectRange(VirtualFrame frame, DynamicObject range) {
        int begin = toInt(frame, Layouts.OBJECT_RANGE.getBegin(range));
        int end = toInt(frame, Layouts.OBJECT_RANGE.getEnd(range));
        boolean excludedEnd = Layouts.OBJECT_RANGE.getExcludedEnd(range);
        return Layouts.INTEGER_FIXNUM_RANGE.createIntegerFixnumRange(
                coreLibrary().getIntegerFixnumRangeFactory(), excludedEnd, begin, end);
    }

    private int toInt(VirtualFrame frame, Object indexObject) {
        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toIntNode = insert(ToIntNode.create());
        }
        return toIntNode.doInt(frame, indexObject);
    }

}
