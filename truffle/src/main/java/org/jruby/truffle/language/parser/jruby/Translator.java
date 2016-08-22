/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.parser.jruby;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.InvalidSourcePosition;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.CheckArityNode;
import org.jruby.truffle.language.arguments.CheckKeywordArityNode;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.language.literal.NilLiteralNode;
import org.jruby.truffle.language.methods.Arity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    public static final Set<String> ALWAYS_DEFINED_GLOBALS = new HashSet<>(Arrays.asList("$~", "$!"));
    public static final Set<String> FRAME_LOCAL_GLOBAL_VARIABLES = new HashSet<>(Arrays.asList("$_", "$+", "$&", "$`", "$'"));

    protected final Node currentNode;
    protected final RubyContext context;
    protected final Source source;

    protected Deque<SourceSection> parentSourceSection = new ArrayDeque<>();

    public Translator(Node currentNode, RubyContext context, Source source) {
        this.currentNode = currentNode;
        this.context = context;
        this.source = source;
    }

    public static RubyNode sequence(RubyContext context, SourceSection sourceSection, List<RubyNode> sequence) {
        final List<RubyNode> flattened = flatten(context, sequence, true);

        if (flattened.isEmpty()) {
            return new NilLiteralNode(context, sourceSection, true);
        } else if (flattened.size() == 1) {
            return flattened.get(0);
        } else {
            final RubyNode[] flatSequence = flattened.toArray(new RubyNode[flattened.size()]);
            return new SequenceNode(context, enclosing(sourceSection, flatSequence), flatSequence);
        }
    }

    public static SourceSection enclosing(SourceSection base, SourceSection... sourceSections) {
        for (SourceSection sourceSection : sourceSections) {
            if (base == null) {
                base = sourceSection;
            } else {
                break;
            }
        }

        if (base == null) {
            return null;
        }

        if (base.getSource() == null) {
            return base;
        }

        if (sourceSections.length == 0) {
            return base;
        }

        int startLine = base.getStartLine();
        int endLine = base.getEndLine();

        for (SourceSection sourceSection : sourceSections) {
            if (sourceSection == null) {
                continue;
            }

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

        return base.getSource().createSection(index, length);
    }

    public static SourceSection enclosing(SourceSection base, RubyNode[] sequence) {
        final SourceSection[] sourceSections = new SourceSection[sequence.length];

        for (int n = 0; n < sequence.length; n++) {
            sourceSections[n] = sequence[n].getEncapsulatingSourceSection();
        }

        return enclosing(base, sourceSections);
    }

    private static List<RubyNode> flatten(RubyContext context, List<RubyNode> sequence, boolean allowTrailingNil) {
        final List<RubyNode> flattened = new ArrayList<>();

        for (int n = 0; n < sequence.size(); n++) {
            final boolean lastNode = n == sequence.size() - 1;
            final RubyNode node = sequence.get(n);

            if (node instanceof NilLiteralNode && ((NilLiteralNode) node).isImplicit()) {
                if (allowTrailingNil && lastNode) {
                    flattened.add(node);
                }
            } else if (node instanceof SequenceNode) {
                flattened.addAll(flatten(context, Arrays.asList(((SequenceNode) node).getSequence()), lastNode));
            } else {
                flattened.add(node);
            }
        }

        return flattened;
    }

    protected SourceSection translate(ISourcePosition sourcePosition) {
        return translate(source, sourcePosition);
    }

    private SourceSection translate(Source source, ISourcePosition sourcePosition) {
        if (sourcePosition == InvalidSourcePosition.INSTANCE) {
            if (parentSourceSection.peek() == null) {
                throw new UnsupportedOperationException("Truffle doesn't want invalid positions - find a way to give me a real position!");
            } else {
                return parentSourceSection.peek();
            }
        } else {
            return source.createSection("(identifier)", sourcePosition.getLine() + 1);
        }
    }

    protected RubyNode nilNode(SourceSection sourceSection) {
        return new NilLiteralNode(context, sourceSection, false);
    }

    protected RubyNode translateNodeOrNil(SourceSection sourceSection, org.jruby.ast.Node node) {
        final RubyNode rubyNode;
        if (node != null) {
            rubyNode = node.accept(this);
        } else {
            rubyNode = nilNode(sourceSection);
        }
        return rubyNode;
    }

    public static RubyNode createCheckArityNode(RubyContext context, SourceSection sourceSection, Arity arity) {
        if (!arity.acceptsKeywords()) {
            return new CheckArityNode(arity);
        } else {
            return new CheckKeywordArityNode(context, sourceSection, arity);
        }
    }

    protected void setSourceSection(RubyNode node, SourceSection sourceSection) {
        node.unsafeSetSourceSection(sourceSection);
    }

}
