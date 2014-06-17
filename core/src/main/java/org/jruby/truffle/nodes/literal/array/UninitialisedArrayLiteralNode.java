/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal.array;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.util.cli.Options;

@NodeInfo(shortName = "uninit-array-literal")
public class UninitialisedArrayLiteralNode extends ArrayLiteralNode {

    public UninitialisedArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
        super(context, sourceSection, values);
    }

    @ExplodeLoop
    @Override
    public RubyArray executeArray(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final Object[] executedValues = new Object[values.length];

        for (int n = 0; n < values.length; n++) {
            executedValues[n] = values[n].execute(frame);
        }

        final RubyArray array = RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), executedValues);
        final Object store = array.getStore();

        if (store instanceof int[]) {
            replace(new IntegerFixnumArrayLiteralNode(getContext(), getSourceSection(), values));
        } else if (store instanceof long[]) {
            replace(new LongFixnumArrayLiteralNode(getContext(), getSourceSection(), values));
        } else if (store instanceof double[]) {
            replace(new FloatArrayLiteralNode(getContext(), getSourceSection(), values));
        } else {
            replace(new ObjectArrayLiteralNode(getContext(), getSourceSection(), values));
        }

        return array;
    }

}
