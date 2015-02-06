/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.array;

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.literal.FixnumLiteralNode;
import org.jruby.truffle.runtime.RubyContext;

public abstract class PrimitiveArrayNodeFactory {

    public static ArrayReadNode read(RubyContext context, SourceSection sourceSection, RubyNode array, int index) {
        return ArrayReadNodeFactory.create(context, sourceSection, array,
                new FixnumLiteralNode.IntegerFixnumLiteralNode(context, sourceSection, index));
    }

}
