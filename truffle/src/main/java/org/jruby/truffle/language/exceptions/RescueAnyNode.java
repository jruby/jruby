/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.exceptions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class RescueAnyNode extends RescueNode {

    public RescueAnyNode(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection, body);
    }

    @Override
    public boolean canHandle(VirtualFrame frame, DynamicObject exception) {
        return getIsANode().executeIsA(exception, coreLibrary().getStandardErrorClass());
    }

}
