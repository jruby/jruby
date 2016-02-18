/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.literal.LiteralNode;
import org.jruby.truffle.language.literal.NilNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A sequence of statements to be executed in serial.
 */
@NodeInfo(cost = NodeCost.NONE)
public final class SequenceNode extends RubyNode {

    @Children private final RubyNode[] body;

    public static RubyNode sequenceNoFlatten(RubyContext context, SourceSection sourceSection, RubyNode... sequence) {
        return new SequenceNode(context, sourceSection, sequence);
    }

    public static RubyNode sequenceNoFlatten(RubyContext context, SourceSection sourceSection, List<RubyNode> sequence) {
        return sequenceNoFlatten(context, sourceSection, sequence.toArray(new RubyNode[sequence.size()]));
    }

    public static RubyNode sequence(RubyContext context, SourceSection sourceSection, RubyNode... sequence) {
        return sequence(context, sourceSection, Arrays.asList(sequence));
    }

    public static RubyNode sequence(RubyContext context, SourceSection sourceSection, List<RubyNode> sequence) {
        final List<RubyNode> flattened = flatten(context, sequence, true);

        if (flattened.isEmpty()) {
            return new NilNode(context, sourceSection);
        } else if (flattened.size() == 1) {
            return flattened.get(0);
        } else {
            final RubyNode[] flatSequence = flattened.toArray(new RubyNode[flattened.size()]);
            return new SequenceNode(context, enclosing(sourceSection, flatSequence), flatSequence);
        }
    }

    public static SourceSection enclosing(SourceSection base, SourceSection... sourceSections) {
        if (base.getSource() == null) {
            return base;
        }

        if (sourceSections.length == 0) {
            return base;
        }

        int startLine = base.getStartLine();
        int endLine = base.getEndLine();

        for (SourceSection sourceSection : sourceSections) {
            startLine = Math.min(startLine, sourceSection.getStartLine());

            final int nodeEndLine;

            if (sourceSection.getSource() == null) {
                nodeEndLine = sourceSection.getStartLine();
            } else {
                nodeEndLine = sourceSection.getEndLine();
            }

            endLine = Math.max(endLine, nodeEndLine);
        }

        final int index = base.getSource().getLineStartOffset(startLine);

        int length = 0;

        for (int n = startLine; n <= endLine; n++) {
            // + 1 because the line length doesn't include any newlines
            length += base.getSource().getLineLength(n) + 1;
        }

        length = Math.min(length, base.getSource().getLength() - index);
        length = Math.max(0, length);

        return base.getSource().createSection(base.getIdentifier(), index, length);
    }

    public static SourceSection enclosing(SourceSection base, RubyNode[] sequence) {
        if (base.getSource() == null) {
            return base;
        }

        if (sequence.length == 0) {
            return base;
        }

        int startLine = base.getStartLine();
        int endLine = base.getEndLine();

        for (RubyNode node : sequence) {
            final SourceSection nodeSourceSection = node.getEncapsulatingSourceSection();

            startLine = Math.min(startLine, nodeSourceSection.getStartLine());

            final int nodeEndLine;

            if (nodeSourceSection.getSource() == null) {
                nodeEndLine = nodeSourceSection.getStartLine();
            } else {
                nodeEndLine = nodeSourceSection.getEndLine();
            }

            endLine = Math.max(endLine, nodeEndLine);
        }

        final int index = base.getSource().getLineStartOffset(startLine);

        int length = 0;

        for (int n = startLine; n <= endLine; n++) {
            // + 1 because the line length doesn't include any newlines
            length += base.getSource().getLineLength(n) + 1;
        }

        length = Math.min(length, base.getSource().getLength() - index);
        length = Math.max(0, length);

        return base.getSource().createSection(base.getIdentifier(), index, length);
    }

    private static List<RubyNode> flatten(RubyContext context, List<RubyNode> sequence, boolean allowTrailingNil) {
        final List<RubyNode> flattened = new ArrayList<>();

        for (int n = 0; n < sequence.size(); n++) {
            final boolean lastNode = n == sequence.size() - 1;
            final RubyNode node = sequence.get(n);

            if (node instanceof NilNode || (node instanceof LiteralNode && ((LiteralNode) node).getObject() == context.getCoreLibrary().getNilObject())) {
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

    public RubyNode[] getSequence() {
        return body;
    }
}
