/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;

public class ReadCallerFrameNode extends RubyNode {

    private final ConditionProfile callerFrameProfile = ConditionProfile.createBinaryProfile();

    @Override
    public Object execute(VirtualFrame frame) {
        final Object callerFrame = RubyArguments.getCallerFrame(frame);

        if (callerFrameProfile.profile(callerFrame == null)) {
            return NotProvided.INSTANCE;
        } else {
            return callerFrame;
        }
    }

}
