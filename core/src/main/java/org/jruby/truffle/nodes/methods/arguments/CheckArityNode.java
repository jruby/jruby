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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.*;

/**
 * Check arguments meet the arity of the method.
 */
@NodeInfo(shortName = "check-arity")
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
            throw new RaiseException(getContext().getCoreLibrary().argumentError(given, arity.getMaximum()));
        }
    }

    private boolean checkArity(int given) {
        if (arity.getMinimum() != Arity.NO_MINIMUM && given < arity.getMinimum()) {
            return false;
        }

        if (arity.getMaximum() != Arity.NO_MAXIMUM && given > arity.getMaximum()) {
            return false;
        }

        return true;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return NilPlaceholder.INSTANCE;
    }

}
