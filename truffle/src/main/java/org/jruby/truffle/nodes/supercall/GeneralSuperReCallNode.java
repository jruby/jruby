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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

import java.util.Arrays;

public class GeneralSuperReCallNode extends AbstractGeneralSuperCallNode {

    private final boolean inBlock;
    @Child private RubyNode block;

    public GeneralSuperReCallNode(RubyContext context, SourceSection sourceSection, boolean inBlock, RubyNode block) {
        super(context, sourceSection);
        this.inBlock = inBlock;
        this.block = block;
    }

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

        final Object[] superArguments = Arrays.copyOf(originalArguments, originalArguments.length);

        if (block != null) {
            Object blockObject = block.execute(frame);

            if (blockObject == nil()) {
                blockObject = null;
            }

            superArguments[RubyArguments.BLOCK_INDEX] = blockObject;
        }

        superArguments[RubyArguments.METHOD_INDEX] = superMethod;

        return callNode.call(frame, superArguments);
    }

}
