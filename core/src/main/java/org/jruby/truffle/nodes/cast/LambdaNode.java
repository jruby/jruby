/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

@NodeInfo(shortName = "lambda")
public class LambdaNode extends RubyNode {

    @Child private RubyNode definition;

    public LambdaNode(RubyContext context, SourceSection sourceSection, RubyNode definition) {
        super(context, sourceSection);
        this.definition = adoptChild(definition);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.LAMBDA, frame.getArguments(RubyArguments.class).getSelf(), null, (RubyMethod) definition.execute(frame));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        definition.executeVoid(frame);
    }

}
