/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

public class BoxingNode extends RubyNode {

    @Child protected RubyNode child;

    private final BranchProfile boxBranch = new BranchProfile();

    public BoxingNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = adoptChild(child);
    }

    @Override
    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) {
        final Object object = child.execute(frame);

        RubyBasicObject boxedObject;

        if (object instanceof RubyBasicObject) {
            boxedObject = (RubyBasicObject) object;
        } else {
            boxBranch.enter();
            boxedObject = getContext().getCoreLibrary().box(object);
        }

        return boxedObject;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyBasicObject(frame);
    }

}
