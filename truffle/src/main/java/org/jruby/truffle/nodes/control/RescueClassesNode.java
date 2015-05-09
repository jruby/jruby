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
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyException;

/**
 * Rescues any of a set of classes.
 */
public class RescueClassesNode extends RescueNode {

    @Children final RubyNode[] handlingClassNodes;

    public RescueClassesNode(RubyContext context, SourceSection sourceSection, RubyNode[] handlingClassNodes, RubyNode body) {
        super(context, sourceSection, body);
        this.handlingClassNodes = handlingClassNodes;
    }

    @Override
    public boolean canHandle(VirtualFrame frame, RubyException exception) {
        CompilerDirectives.transferToInterpreter();

        final RubyClass exceptionRubyClass = exception.getLogicalClass();

        for (RubyNode handlingClassNode : handlingClassNodes) {
            // TODO(CS): what if we don't get a class?

            final RubyClass handlingClass = (RubyClass) handlingClassNode.execute(frame);

            if (ModuleOperations.assignableTo(exceptionRubyClass, handlingClass)) {
                return true;
            }
        }

        return false;
    }
}
