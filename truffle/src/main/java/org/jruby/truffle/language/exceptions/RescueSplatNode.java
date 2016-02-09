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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.ModuleOperations;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.Layouts;

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
    public boolean canHandle(VirtualFrame frame, DynamicObject exception) {
        CompilerDirectives.transferToInterpreter();

        final DynamicObject handlingClasses = (DynamicObject) handlingClassesArray.execute(frame);

        final DynamicObject exceptionRubyClass = Layouts.BASIC_OBJECT.getLogicalClass(exception);

        for (Object handlingClass : ArrayOperations.toIterable(handlingClasses)) {
            if (ModuleOperations.assignableTo(exceptionRubyClass, (DynamicObject) handlingClass)) {
                return true;
            }
        }

        return false;
    }

}
