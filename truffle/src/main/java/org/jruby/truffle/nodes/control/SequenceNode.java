/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.defined.DefinedWrapperNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.runtime.RubyContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A sequence of statements to be executed in serial.
 */
@NodeInfo(cost = NodeCost.NONE)
public final class SequenceNode extends RubyNode {

    @Children private final RubyNode[] body;

    public static RubyNode sequence(RubyContext context, SourceSection sourceSection, RubyNode... sequence) {
        return sequence(context, sourceSection, Arrays.asList(sequence));
    }

    public static RubyNode sequence(RubyContext context, SourceSection sourceSection, List<RubyNode> sequence) {
        final List<RubyNode> flattened = flatten(context, sequence, true);

        if (flattened.isEmpty()) {
            return new DefinedWrapperNode(context, sourceSection,
                    new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                    "nil");
        } else if (flattened.size() == 1) {
            return flattened.get(0);
        } else {
            return new SequenceNode(context, sourceSection, flattened.toArray(new RubyNode[flattened.size()]));
        }
    }

    private static List<RubyNode> flatten(RubyContext context, List<RubyNode> sequence, boolean allowTrailingNil) {
        final List<RubyNode> flattened = new ArrayList<>();

        for (int n = 0; n < sequence.size(); n++) {
            final boolean lastNode = n == sequence.size() - 1;
            final RubyNode node = sequence.get(n);

            if (node instanceof ObjectLiteralNode && ((ObjectLiteralNode) node).getObject() == context.getCoreLibrary().getNilObject()) {
                if (allowTrailingNil && lastNode) {
                    flattened.add(node);
                }
            } else if (node instanceof SequenceNode) {
                flattened.addAll(flatten(context, Arrays.asList(((SequenceNode) node).body), lastNode));
            } else {
                flattened.add(node);
            }
        }

        return flattened;
    }

    protected SequenceNode(RubyContext context, SourceSection sourceSection, RubyNode... body) {
        super(context, sourceSection);
        this.body = body;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        for (int n = 0; n < body.length - 1; n++) {
            body[n].executeVoid(frame);
        }

        return body[body.length - 1].execute(frame);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (int n = 0; n < body.length; n++) {
            body[n].executeVoid(frame);
        }
    }

}
