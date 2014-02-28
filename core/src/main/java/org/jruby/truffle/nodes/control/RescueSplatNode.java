/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.*;

/**
 * Rescue any of several classes, that we get from an expression that evaluates to an array of
 * classes.
 * 
 */
@NodeInfo(shortName = "rescue-splat")
public class RescueSplatNode extends RescueNode {

    @Child RubyNode handlingClassesArray;

    public RescueSplatNode(RubyContext context, SourceSection sourceSection, RubyNode handlingClassesArray, RubyNode body) {
        super(context, sourceSection, body);
        this.handlingClassesArray = adoptChild(handlingClassesArray);
    }

    @ExplodeLoop
    @Override
    public boolean canHandle(VirtualFrame frame, RubyBasicObject exception) {
        final RubyArray handlingClasses = (RubyArray) handlingClassesArray.execute(frame);

        final RubyClass exceptionRubyClass = exception.getRubyClass();

        for (Object handlingClass : handlingClasses.asList()) {
            if (exceptionRubyClass.assignableTo((RubyClass) handlingClass)) {
                return true;
            }
        }

        return false;
    }

}
