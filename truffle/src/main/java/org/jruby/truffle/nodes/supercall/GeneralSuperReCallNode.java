/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

public class GeneralSuperReCallNode extends AbstractGeneralSuperCallNode {

    private final boolean inBlock;

    public GeneralSuperReCallNode(RubyContext context, SourceSection sourceSection, boolean inBlock) {
        super(context, sourceSection);
        this.inBlock = inBlock;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        if (!guard()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookup(frame);
        }

        if (method == null || callNode == null) {
            throw new IllegalStateException("No call node installed");
        }

        final Object[] superArguments;

        if (inBlock) {
            superArguments = RubyArguments.getDeclarationFrame(frame.getArguments()).getArguments();
        } else {
            superArguments = frame.getArguments();
        }

        return callNode.call(frame, superArguments);
    }

}
