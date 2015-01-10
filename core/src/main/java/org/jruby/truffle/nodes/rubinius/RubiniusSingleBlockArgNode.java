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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;

public class RubiniusSingleBlockArgNode extends RubyNode {
    public RubiniusSingleBlockArgNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        /**
         * This is our implementation of Rubinius.single_block_arg.
         *
         * In Rubinius, this method inspects the values yielded to the block, regardless of whether the block
         * captures the values, and returns the first yielded value.
         */

        int userArgumentCount = RubyArguments.getUserArgumentsCount(frame.getArguments());

        if (userArgumentCount == 1) {
            return RubyArguments.getUserArgument(frame.getArguments(), 0);

        } else if (userArgumentCount > 1) {
            Object[] extractedArguments = RubyArguments.extractUserArguments(frame.getArguments());

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), extractedArguments);

        } else {
            return getContext().getCoreLibrary().getNilObject();
        }
    }
}
