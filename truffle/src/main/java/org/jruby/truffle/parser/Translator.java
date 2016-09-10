/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.InvalidSourcePosition;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubySourceSection;
import org.jruby.truffle.language.arguments.CheckArityNode;
import org.jruby.truffle.language.arguments.CheckKeywordArityNode;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadSelfNode;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.language.literal.NilLiteralNode;
import org.jruby.truffle.language.locals.WriteLocalVariableNode;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.objects.SelfNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    public static final Set<String> ALWAYS_DEFINED_GLOBALS = new HashSet<>(Arrays.asList("$!", "$~"));
    public static final Set<String> FRAME_LOCAL_GLOBAL_VARIABLES = new HashSet<>(Arrays.asList("$_", "$+", "$&", "$`", "$'", "$~", "$1", "$2", "$3", "$4", "$5", "$6", "$7", "$8", "$9"));

    protected final Node currentNode;
    protected final RubyContext context;
    protected final Source source;

    protected Deque<RubySourceSection> parentSourceSection = new ArrayDeque<>();

    public Translator(Node currentNode, RubyContext context, Source source) {
        this.currentNode = currentNode;
        this.context = context;
        this.source = source;
    }

    public static RubyNode sequence(RubyContext context, Source source, RubySourceSection sourceSection, List<RubyNode> sequence) {
        final List<RubyNode> flattened = flatten(sequence, true);

        if (flattened.isEmpty()) {
            return new NilLiteralNode(context, sourceSection.toSourceSection(source), true);
        } else if (flattened.size() == 1) {
            return flattened.get(0);
        } else {
            final RubyNode[] flatSequence = flattened.toArray(new RubyNode[flattened.size()]);

            final RubySourceSection enclosingSourceSection = enclosing(sourceSection, flatSequence);

            final SourceSection sequenceSourceSection;

            if (enclosingSourceSection == null) {
                sequenceSourceSection = null;
            } else {
                sequenceSourceSection = enclosingSourceSection.toSourceSection(source);
            }

            return new SequenceNode(sequenceSourceSection, flatSequence);
        }
    }

    public static RubySourceSection enclosing(RubySourceSection base, RubyNode... sequence) {
        if (base == null) {
            return base;
        }

        int startLine = base.getStartLine();
        int endLine = base.getEndLine();

        for (RubyNode node : sequence) {
            final RubySourceSection sourceSection = node.getRubySourceSection();

            if (sourceSection != null) {
                startLine = Integer.min(startLine, sourceSection.getStartLine());
                endLine = Integer.max(endLine, sourceSection.getEndLine());
            }
        }

        return new RubySourceSection(startLine, endLine);
    }

    private static List<RubyNode> flatten(List<RubyNode> sequence, boolean allowTrailingNil) {
        final List<RubyNode> flattened = new ArrayList<>();

        for (int n = 0; n < sequence.size(); n++) {
            final boolean lastNode = n == sequence.size() - 1;
            final RubyNode node = sequence.get(n);

            if (node instanceof NilLiteralNode && ((NilLiteralNode) node).isImplicit()) {
                if (allowTrailingNil && lastNode) {
                    flattened.add(node);
                }
            } else if (node instanceof SequenceNode) {
                flattened.addAll(flatten(Arrays.asList(((SequenceNode) node).getSequence()), lastNode));
            } else {
                flattened.add(node);
            }
        }

        return flattened;
    }

    protected RubySourceSection translate(ISourcePosition sourcePosition) {
        return translate(source, sourcePosition);
    }

    private RubySourceSection translate(Source source, ISourcePosition sourcePosition) {
        if (sourcePosition == InvalidSourcePosition.INSTANCE) {
            if (parentSourceSection.peek() == null) {
                throw new UnsupportedOperationException("Truffle doesn't want invalid positions - find a way to give me a real position!");
            } else {
                return parentSourceSection.peek();
            }
        } else {
            return new RubySourceSection(sourcePosition.getLine() + 1);
        }
    }

    protected RubyNode nilNode(Source source, RubySourceSection sourceSection) {
        return new NilLiteralNode(context, sourceSection.toSourceSection(source), false);
    }

    protected RubyNode translateNodeOrNil(RubySourceSection sourceSection, org.jruby.ast.Node node) {
        final RubyNode rubyNode;
        if (node != null) {
            rubyNode = node.accept(this);
        } else {
            rubyNode = nilNode(source, sourceSection);
        }
        return rubyNode;
    }

    public static RubyNode createCheckArityNode(RubyContext context, Source source, RubySourceSection sourceSection, Arity arity) {
        if (!arity.acceptsKeywords()) {
            return new CheckArityNode(arity);
        } else {
            return new CheckKeywordArityNode(context, sourceSection.toSourceSection(source), arity);
        }
    }

    public SourceSection translateSourceSection(Source source, RubySourceSection sourceSection) {
        if (sourceSection == null) {
            return null;
        } else {
            return sourceSection.toSourceSection(source);
        }
    }

    public static RubyNode loadSelf(RubyContext context, TranslatorEnvironment environment) {
        final FrameSlot slot = environment.getFrameDescriptor().findOrAddFrameSlot(SelfNode.SELF_IDENTIFIER);
        RubySourceSection sourceSection = null;
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, sourceSection, slot, new ProfileArgumentNode(new ReadSelfNode()));
    }

}
