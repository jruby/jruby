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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

public class GeneralSuperReCallNode extends AbstractGeneralSuperCallNode {

    private final boolean inBlock;
    @Children private final RubyNode[] reloadNodes;
    @Child private RubyNode block;

    public GeneralSuperReCallNode(RubyContext context, SourceSection sourceSection, boolean inBlock, RubyNode[] reloadNodes, RubyNode block) {
        super(context, sourceSection);
        this.inBlock = inBlock;
        this.reloadNodes = reloadNodes;
        this.block = block;
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        final Object self = RubyArguments.getSelf(frame.getArguments());

        if (!guard(frame, self)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookup(frame);
        }

        final Object[] originalArguments;

        if (inBlock) {
            originalArguments = RubyArguments.getDeclarationFrame(frame.getArguments()).getArguments();
        } else {
            originalArguments = frame.getArguments();
        }

        final Object[] superArguments = new Object[reloadNodes.length];

        for (int n = 0; n < superArguments.length; n++) {
            superArguments[n] = reloadNodes[n].execute(frame);
        }

        Object blockObject;

        if (block != null) {
            blockObject = block.execute(frame);

            if (blockObject == nil()) {
                blockObject = null;
            }
        } else {
            blockObject = RubyArguments.getBlock(originalArguments);
        }

        return callNode.call(frame, RubyArguments.pack(
                superMethod,
                RubyArguments.getDeclarationFrame(originalArguments),
                RubyArguments.getSelf(originalArguments),
                (RubyProc) blockObject,
                superArguments));
    }

}
