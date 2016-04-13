/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;

public class RaiseIfFrozenNode extends RubyNode {

    @Child private RubyNode child;
    @Child private IsFrozenNode isFrozenNode;

    public RaiseIfFrozenNode(RubyNode child) {
        super(child.getContext(), child.getEncapsulatingSourceSection());
        this.child = child;
        isFrozenNode = IsFrozenNodeGen.create(child.getContext(), child.getEncapsulatingSourceSection(), null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result = child.execute(frame);

        if (isFrozenNode.executeIsFrozen(result)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().frozenError(
                    Layouts.MODULE.getFields(coreLibrary().getLogicalClass(result)).getName(), this));
        }

        return result;
    }
}
