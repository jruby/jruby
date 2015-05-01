/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;

public class LambdaNode extends RubyNode {

    // TODO(CS): this should be a lambda definition node, alongside block definition node

    @Child private RubyNode definition;

    public LambdaNode(RubyContext context, SourceSection sourceSection, RubyNode definition) {
        super(context, sourceSection);
        this.definition = definition;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final InternalMethod method = (InternalMethod) definition.execute(frame);

        // TODO(CS): not sure we're closing over the correct state here

        return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.LAMBDA,
                method.getSharedMethodInfo(), method.getCallTarget(), method.getCallTarget(), method.getCallTarget(),
                method.getDeclarationFrame(), method, RubyArguments.getSelf(frame.getArguments()), null);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        definition.executeVoid(frame);
    }

}
