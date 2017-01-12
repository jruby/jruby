/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;

public class SingleBlockArgNode extends RubyNode {
    private final ConditionProfile emptyArgsProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile singleArgProfile = ConditionProfile.createBinaryProfile();

    @Override
    public Object execute(VirtualFrame frame) {
        /**
         * This is our implementation of Truffle.single_block_arg.
         *
         * In Rubinius, this method inspects the values yielded to the block, regardless of whether the block
         * captures the values, and returns the first value in the list of values yielded to the block.
         *
         * NB: In our case the arguments have already been destructured by the time this node is encountered.
         * Thus, we don't need to do the destructuring work that Rubinius would do and in the case that we receive
         * multiple arguments we need to reverse the destructuring by collecting the values into an array.
         */

        int userArgumentCount = RubyArguments.getArgumentsCount(frame);

        if (emptyArgsProfile.profile(userArgumentCount == 0)) {
            return nil();
        } else {
            if (singleArgProfile.profile(userArgumentCount == 1)) {
                return RubyArguments.getArgument(frame, 0);

            } else {
                Object[] extractedArguments = RubyArguments.getArguments(frame);

                return createArray(extractedArguments, userArgumentCount);
            }
        }
    }
}
