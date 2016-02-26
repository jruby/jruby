/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.Arity;

public class CheckArityNode extends RubyNode {

    private final Arity arity;

    public CheckArityNode(RubyContext context, SourceSection sourceSection, Arity arity) {
        super(context, sourceSection);
        this.arity = arity;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        final int given = RubyArguments.getArgumentsCount(frame);
        if (!checkArity(given)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().argumentError(given, arity.getRequired(), this));
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException("CheckArity should be call with executeVoid()");
    }

    private boolean checkArity(int given) {
        final int required = arity.getRequired();
        if (required != 0 && given < required) {
            return false;
        } else if (!arity.hasRest() && given > required + arity.getOptional()) {
            return false;
        } else {
            return true;
        }
    }

}
