/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format;

import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.control.RepeatExplodedNode;
import org.jruby.truffle.core.format.control.RepeatLoopNode;
import org.jruby.truffle.core.format.control.SequenceNode;
import org.jruby.truffle.core.format.control.StarNode;
import org.jruby.truffle.core.format.pack.SimplePackParser;

import java.util.Deque;
import java.util.List;

public class SharedTreeBuilder {

    private final RubyContext context;

    public SharedTreeBuilder(RubyContext context) {
        this.context = context;
    }

    public FormatNode finishSubSequence(Deque<List<FormatNode>> sequenceStack, int count) {
        final List<FormatNode> sequence = sequenceStack.pop();
        final SequenceNode sequenceNode = new SequenceNode(sequence.toArray(new FormatNode[sequence.size()]));

        if (count == SimplePackParser.COUNT_NONE) {
            return sequenceNode;
        } else {
            return createRepeatNode(count, sequenceNode);
        }
    }

    public FormatNode applyCount(int count, FormatNode node) {
        switch (count) {
            case SimplePackParser.COUNT_NONE:
                return node;

            case SimplePackParser.COUNT_STAR:
                return new StarNode(node);

            default:
                return createRepeatNode(count, node);
        }
    }

    private FormatNode createRepeatNode(int count, FormatNode node) {
        if (count > context.getOptions().PACK_UNROLL_LIMIT) {
            return new RepeatLoopNode(count, node);
        } else {
            return new RepeatExplodedNode(count, node);
        }
    }

    public StarLength parseCountContext(int count) {
        final boolean star;
        final int length;

        if (count == SimplePackParser.COUNT_NONE) {
            star = false;
            length = 1;
        } else if (count == SimplePackParser.COUNT_STAR) {
            star = true;
            length = 0;
        } else {
            star = false;
            length = count;
        }

        return new StarLength(star, length);
    }

    public static class StarLength {

        private final boolean star;
        private final int length;

        public StarLength(boolean star, int length) {
            this.star = star;
            this.length = length;
        }

        public boolean isStar() {
            return star;
        }

        public int getLength() {
            return length;
        }
    }

}
