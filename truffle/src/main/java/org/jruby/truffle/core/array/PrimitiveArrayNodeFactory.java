/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.literal.IntegerFixnumLiteralNode;

public abstract class PrimitiveArrayNodeFactory {

    /**
     * Create a node to read from an array with a constant denormalized index.
     */
    public static RubyNode read(RubyContext context, SourceSection sourceSection, RubyNode array, int index) {
        final RubyNode literalIndex = new IntegerFixnumLiteralNode(context, sourceSection, index);

        if (index >= 0) {
            return ArrayReadNormalizedNodeGen.create(context, sourceSection, array, literalIndex);
        } else {
            return ArrayReadDenormalizedNodeGen.create(context, sourceSection, array, literalIndex);
        }
    }

    /**
     * Create a node to read a slice from an array with a constant denormalized start and exclusive end.
     */
    public static RubyNode readSlice(RubyContext context, SourceSection sourceSection, RubyNode array, int start, int exclusiveEnd) {
        final RubyNode literalStart = new IntegerFixnumLiteralNode(context, sourceSection, start);
        final RubyNode literalExclusiveEnd = new IntegerFixnumLiteralNode(context, sourceSection, exclusiveEnd);

        if (start >= 0 && exclusiveEnd >= 0) {
            return ArrayReadSliceNormalizedNodeGen.create(context, sourceSection, array, literalStart, literalExclusiveEnd);
        } else {
            return ArrayReadSliceDenormalizedNodeGen.create(context, sourceSection, array, literalStart, literalExclusiveEnd);
        }
    }

}
