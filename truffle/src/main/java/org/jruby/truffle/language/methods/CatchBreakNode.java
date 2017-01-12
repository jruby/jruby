/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.BreakException;
import org.jruby.truffle.language.control.BreakID;

public class CatchBreakNode extends RubyNode {

    private final BreakID breakID;

    @Child private RubyNode body;

    private final ConditionProfile matchingBreakProfile = ConditionProfile.createCountingProfile();

    public CatchBreakNode(BreakID breakID, RubyNode body) {
        this.breakID = breakID;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (BreakException e) {
            if (matchingBreakProfile.profile(e.getBreakID() == breakID)) {
                return e.getResult();
            } else {
                throw e;
            }
        }
    }

}
