/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.arguments;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.*;

/**
 * Check arguments meet the arity of the method.
 */
public class CheckArityNode extends RubyNode {

    private final Arity arity;

    public CheckArityNode(RubyContext context, SourceSection sourceSection, Arity arity) {
        super(context, sourceSection);
        this.arity = arity;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        final int given = RubyArguments.getUserArgumentsCount(frame.getArguments());

        if (!checkArity(given)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getRequired(), this));
        }
    }

    private boolean checkArity(int given) {
        if (arity.hasKeywords()) {
            // TODO(CS): TODO
            return true;
        }

        if (arity.getRequired() != 0 && given < arity.getRequired()) {
            return false;
        } else if (!arity.allowsMore() && given > arity.getRequired() + arity.getOptional()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return getContext().getCoreLibrary().getNilObject();
    }

}
