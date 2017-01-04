/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.language.RubyNode;

public class RescueAnyNode extends RescueNode {

    public RescueAnyNode(RubyNode rescueBody) {
        super(rescueBody);
    }

    @Override
    public boolean canHandle(VirtualFrame frame, DynamicObject exception) {
        return matches(frame, exception, coreLibrary().getStandardErrorClass());
    }

}
