/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;

public class RaiseIfFrozenNode extends RubyNode {

    private final BranchProfile errorProfile = BranchProfile.create();

    @Child private RubyNode child;
    @Child private IsFrozenNode isFrozenNode = IsFrozenNodeGen.create(null);

    public RaiseIfFrozenNode(RubyNode child) {
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result = child.execute(frame);

        if (isFrozenNode.executeIsFrozen(result)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().frozenError(result, this));
        }

        return result;
    }
}
