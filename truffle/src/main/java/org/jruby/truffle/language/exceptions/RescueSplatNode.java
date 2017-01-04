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
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.language.RubyNode;

public class RescueSplatNode extends RescueNode {

    @Child private RubyNode handlingClassesArray;

    public RescueSplatNode(RubyNode handlingClassesArray, RubyNode rescueBody) {
        super(rescueBody);
        this.handlingClassesArray = handlingClassesArray;
    }

    @Override
    public boolean canHandle(VirtualFrame frame, DynamicObject exception) {
        final DynamicObject handlingClasses = (DynamicObject) handlingClassesArray.execute(frame);

        for (Object handlingClass : ArrayOperations.toIterable(handlingClasses)) {
            if (matches(frame, exception, handlingClass)) {
                return true;
            }
        }

        return false;
    }

}
