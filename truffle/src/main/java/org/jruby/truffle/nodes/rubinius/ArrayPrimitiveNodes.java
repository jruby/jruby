/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyNilClass;

public abstract class ArrayPrimitiveNodes {

    @RubiniusPrimitive(name = "tuple_copy_from")
    public static abstract class TupleCopyFromPrimitiveNode extends RubiniusPrimitiveNode {

        public TupleCopyFromPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object tupleCopyFrom() {
            return null;
        }

    }

}
