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
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;

public class RubiniusSingleBlockArgNode extends RubyNode {
    private final ConditionProfile emptyArgsProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile singleArgProfile = ConditionProfile.createBinaryProfile();

    public RubiniusSingleBlockArgNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        /**
         * This is our implementation of Rubinius.single_block_arg.
         *
         * In Rubinius, this method inspects the values yielded to the block, regardless of whether the block
         * captures the values, and returns the first value in the list of values yielded to the block.
         *
         * NB: In our case the arguments have already been destructured by the time this node is encountered.
         * Thus, we don't need to do the destructuring work that Rubinius would do and in the case that we receive
         * multiple arguments we need to reverse the destructuring by collecting the values into an array.
         */

        int userArgumentCount = RubyArguments.getUserArgumentsCount(frame.getArguments());

        if (emptyArgsProfile.profile(userArgumentCount == 0)) {
            return nil();
        } else {
            if (singleArgProfile.profile(userArgumentCount == 1)) {
                return RubyArguments.getUserArgument(frame.getArguments(), 0);

            } else {
                Object[] extractedArguments = RubyArguments.extractUserArguments(frame.getArguments());

                return new RubyArray(getContext().getCoreLibrary().getArrayClass(), extractedArguments, userArgumentCount);
            }
        }
    }
}
