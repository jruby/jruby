/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class ArrayPrimitiveNodes {

    @RubiniusPrimitive(name = "tuple_copy_from")
    public static abstract class TupleCopyFromPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public Object tupleCopyFrom() {
            return null;
        }

    }

}
