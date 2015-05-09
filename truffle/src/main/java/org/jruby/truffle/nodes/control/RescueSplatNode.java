/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyException;

/**
 * Rescue any of several classes, that we get from an expression that evaluates to an array of
 * classes.
 * 
 */
public class RescueSplatNode extends RescueNode {

    @Child RubyNode handlingClassesArray;

    public RescueSplatNode(RubyContext context, SourceSection sourceSection, RubyNode handlingClassesArray, RubyNode body) {
        super(context, sourceSection, body);
        this.handlingClassesArray = handlingClassesArray;
    }

    @Override
    public boolean canHandle(VirtualFrame frame, RubyException exception) {
        CompilerDirectives.transferToInterpreter();

        final RubyArray handlingClasses = (RubyArray) handlingClassesArray.execute(frame);

        final RubyClass exceptionRubyClass = exception.getLogicalClass();

        for (Object handlingClass : handlingClasses.slowToArray()) {
            if (ModuleOperations.assignableTo(exceptionRubyClass, (RubyClass) handlingClass)) {
                return true;
            }
        }

        return false;
    }

}
