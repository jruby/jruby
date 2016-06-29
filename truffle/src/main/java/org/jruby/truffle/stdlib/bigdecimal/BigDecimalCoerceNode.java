/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.bigdecimal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NodeChildren({
        @NodeChild(value = "value", type = RubyNode.class),
        @NodeChild(value = "roundingMode", type = RoundModeNode.class),
        @NodeChild(value = "cast", type = BigDecimalCastNode.class, executeWith = {"value", "roundingMode"})

})
public abstract class BigDecimalCoerceNode extends RubyNode {

    @Child private CreateBigDecimalNode createBigDecimal;

    public static BigDecimalCoerceNode create(RubyNode value) {
        return BigDecimalCoerceNodeGen.create(value,
                RoundModeNodeFactory.create(),
                BigDecimalCastNodeGen.create(null, null));
    }

    protected DynamicObject createBigDecimal(VirtualFrame frame, Object value) {
        if (createBigDecimal == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            createBigDecimal = insert(CreateBigDecimalNodeFactory.create(null, null, null));
        }

        return createBigDecimal.executeCreate(frame, value);
    }

    public abstract DynamicObject executeBigDecimal(VirtualFrame frame, RoundingMode roundingMode, Object value);

    @Specialization
    public DynamicObject doBigDecimal(VirtualFrame frame, Object value, RoundingMode roundingMode, BigDecimal cast) {
        return createBigDecimal(frame, cast);
    }

    @Specialization(guards = {
            "isRubyBigDecimal(value)",
            "isNil(cast)"
    })
    public Object doBigDecimal(DynamicObject value, RoundingMode roundingMode, DynamicObject cast) {
        return value;
    }

    // TODO (pitr 22-Jun-2015): deal with not-coerce-able values

}
