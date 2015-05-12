/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ValueProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;

public class WritePreArgumentNode extends RubyNode {

    @Child private RubyNode rhs;

    private final int index;

    public WritePreArgumentNode(RubyContext context, SourceSection sourceSection, int index, RubyNode rhs) {
        super(context, sourceSection);
        this.index = index;
        this.rhs = rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        RubyArguments.setUserArgument(frame.getArguments(), index, rhs.execute(frame));
        return nil();
    }

}
