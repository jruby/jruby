/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.literal.IntegerFixnumLiteralNode;

public abstract class PrimitiveArrayNodeFactory {

    /**
     * Create a node to read from an array with a constant denormalized index.
     */
    public static RubyNode read(RubyNode array, int index) {
        final RubyNode literalIndex = new IntegerFixnumLiteralNode(index);

        if (index >= 0) {
            return ArrayReadNormalizedNodeGen.create(array, literalIndex);
        } else {
            return ArrayReadDenormalizedNodeGen.create(array, literalIndex);
        }
    }

    /**
     * Create a node to read a slice from an array with a constant denormalized start and exclusive end.
     */
    public static RubyNode readSlice(RubyNode array, int start, int exclusiveEnd) {
        final RubyNode literalStart = new IntegerFixnumLiteralNode(start);
        final RubyNode literalExclusiveEnd = new IntegerFixnumLiteralNode(exclusiveEnd);

        if (start >= 0 && exclusiveEnd >= 0) {
            return ArrayReadSliceNormalizedNodeGen.create(array, literalStart, literalExclusiveEnd);
        } else {
            return ArrayReadSliceDenormalizedNodeGen.create(array, literalStart, literalExclusiveEnd);
        }
    }

}
