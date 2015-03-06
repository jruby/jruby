/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;

/**
 * Catch a {@code return} jump at the root of a method, and report it as an error.
 */
public class CatchReturnAsErrorNode extends RubyNode {

    @Child private RubyNode body;

    public CatchReturnAsErrorNode(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation("4981e67b94254a73ac0f735a5d6d6155");

        try {
            return body.execute(frame);
        } catch (ReturnException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().unexpectedReturn(this));
        }
    }

}
