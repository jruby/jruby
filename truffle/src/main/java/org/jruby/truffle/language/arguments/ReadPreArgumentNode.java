/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;

public class ReadPreArgumentNode extends RubyNode {

    private final int index;

    private final BranchProfile outOfRangeProfile = BranchProfile.create();
    private final MissingArgumentBehavior missingArgumentBehavior;

    public ReadPreArgumentNode(int index,
                               MissingArgumentBehavior missingArgumentBehavior) {
        this.index = index;
        this.missingArgumentBehavior = missingArgumentBehavior;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (index < RubyArguments.getArgumentsCount(frame)) {
            return RubyArguments.getArgument(frame, index);
        }

        outOfRangeProfile.enter();

        switch (missingArgumentBehavior) {
            case RUNTIME_ERROR:
                throw new IndexOutOfBoundsException();

            case UNDEFINED:
                return NotProvided.INSTANCE;

            case NIL:
                return nil();

            default:
                throw new UnsupportedOperationException("unknown missing argument behaviour");
        }
    }

}
