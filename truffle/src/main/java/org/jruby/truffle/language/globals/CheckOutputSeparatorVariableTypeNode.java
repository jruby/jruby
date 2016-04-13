/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class CheckOutputSeparatorVariableTypeNode extends RubyNode {

    @Child private RubyNode child;

    private final BranchProfile unsuitableTypeProfile = BranchProfile.create();

    public CheckOutputSeparatorVariableTypeNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    public Object execute(VirtualFrame frame) {
        final Object childValue = child.execute(frame);

        if (!RubyGuards.isRubyString(childValue) && !isNil(childValue)) {
            unsuitableTypeProfile.enter();
            throw new RaiseException(coreLibrary().typeErrorMustBe("$,", "String", this));
        }

        return childValue;
    }

}
