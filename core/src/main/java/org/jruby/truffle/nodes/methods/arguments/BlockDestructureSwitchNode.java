/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.arguments;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.respondto.RespondToNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.array.*;
import org.jruby.truffle.runtime.methods.Arity;

/**
 * Switches between loading arguments as normal and doing a destructure. See
 * testBlockArgumentsDestructure and MethodTranslator.
 */
@NodeInfo(shortName = "block-destructure-switch")
public class BlockDestructureSwitchNode extends RubyNode {

    private final Arity arity;
    @Child protected RubyNode loadIndividualArguments;
    @Child protected RespondToNode respondToCheck;
    @Child protected RubyNode destructureArguments;
    @Child protected RubyNode body;

    public BlockDestructureSwitchNode(RubyContext context, SourceSection sourceSection, Arity arity, RubyNode loadIndividualArguments, RespondToNode respondToCheck, RubyNode destructureArguments, RubyNode body) {
        super(context, sourceSection);
        this.arity = arity;
        this.loadIndividualArguments = adoptChild(loadIndividualArguments);
        this.respondToCheck = adoptChild(respondToCheck);
        this.destructureArguments = adoptChild(destructureArguments);
        this.body = adoptChild(body);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (shouldDestructure(frame)) {
            destructureArguments.executeVoid(frame);
        } else {
            loadIndividualArguments.executeVoid(frame);
        }

        return body.execute(frame);
    }

    private boolean shouldDestructure(VirtualFrame frame) {
        final RubyArguments arguments = frame.getArguments(RubyArguments.class);

        // If we only accept one argument, there's never any need to destructure

        if (arity.getMinimum() == 1 && arity.getMaximum() == 1) {
            return false;
        }

        // If the caller supplied no arguments, or more than one argument, there's no need to destructure this time

        if (arguments.getArguments().length != 1) {
            return false;
        }

        // If the single argument is a RubyArray, destructure
        // TODO(CS): can we not just reply on the respondToCheck? Should experiment.

        if (arguments.getArguments()[0] instanceof RubyArray) {
            return true;
        }

        // If the single argument responds to #to_ary, then destructure

        return respondToCheck.executeBoolean(frame);
    }

}
