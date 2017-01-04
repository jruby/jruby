/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class CheckRecordSeparatorVariableTypeNode extends RubyNode {

    @Child private RubyNode child;

    private final BranchProfile unsuitableTypeProfile = BranchProfile.create();

    public CheckRecordSeparatorVariableTypeNode(RubyNode child) {
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object childValue = child.execute(frame);

        if (!RubyGuards.isRubyString(childValue) && !isNil(childValue)) {
            unsuitableTypeProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorMustBe("$/", "String", this));
        }

        return childValue;
    }

}
