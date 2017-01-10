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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.language.arguments.CheckArityNode;
import org.jruby.truffle.language.arguments.CheckKeywordArityNode;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadSelfNode;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.language.literal.NilLiteralNode;
import org.jruby.truffle.language.locals.WriteLocalVariableNode;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.objects.SelfNode;
import org.jruby.truffle.parser.ast.NilImplicitParseNode;
import org.jruby.truffle.parser.ast.ParseNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Translator extends org.jruby.truffle.parser.ast.visitor.AbstractNodeVisitor<RubyNode> {

    public static final Set<String> FRAME_LOCAL_GLOBAL_VARIABLES = new HashSet<>(
            Arrays.asList("$_", "$~", "$+", "$&", "$`", "$'", "$1", "$2", "$3", "$4", "$5", "$6", "$7", "$8", "$9"));
    static final Set<String> READ_ONLY_GLOBAL_VARIABLES = new HashSet<>(
            Arrays.asList("$:", "$LOAD_PATH", "$-I", "$\"", "$LOADED_FEATURES", "$<", "$FILENAME", "$?", "$-a", "$-l", "$-p", "$!"));
    static final Set<String> ALWAYS_DEFINED_GLOBALS = new HashSet<>(Arrays.asList("$!", "$~"));
    static final Set<String> THREAD_LOCAL_GLOBAL_VARIABLES = new HashSet<>(Arrays.asList("$!", "$?")); // "$_"

    static final Map<String, String> GLOBAL_VARIABLE_ALIASES = new HashMap<>();
    static {
        Map<String, String> m = GLOBAL_VARIABLE_ALIASES;
        m.put("$-I", "$LOAD_PATH");
        m.put("$:", "$LOAD_PATH");
        m.put("$-d", "$DEBUG");
        m.put("$-v", "$VERBOSE");
        m.put("$-w", "$VERBOSE");
        m.put("$-0", "$/");
        m.put("$RS", "$/");
        m.put("$INPUT_RECORD_SEPARATOR", "$/");
        m.put("$>", "$stdout");
        m.put("$PROGRAM_NAME", "$0");
    }

    protected final Node currentNode;
    protected final RubyContext context;
    protected final Source source;
    protected final ParserContext parserContext;

    public Translator(Node currentNode, RubyContext context, Source source, ParserContext parserContext) {
        this.currentNode = currentNode;
        this.context = context;
        this.source = source;
        this.parserContext = parserContext;
    }

    public static RubyNode sequence(SourceIndexLength sourceSection, List<RubyNode> sequence) {
        final List<RubyNode> flattened = flatten(sequence, true);

        if (flattened.isEmpty()) {
            final RubyNode literal = new NilLiteralNode(true);
            literal.unsafeSetSourceSection(sourceSection);
            return literal;
        } else if (flattened.size() == 1) {
            return flattened.get(0);
        } else {
            final RubyNode[] flatSequence = flattened.toArray(new RubyNode[flattened.size()]);

            final SourceIndexLength enclosingSourceSection = enclosing(sourceSection, flatSequence);
            return withSourceSection(enclosingSourceSection, new SequenceNode(flatSequence));
        }
    }

    public static SourceIndexLength enclosing(SourceIndexLength base, RubyNode... sequence) {
        if (base == null) {
            return base;
        }

        int start = base.getCharIndex();
        int end = base.getCharEnd();

        for (RubyNode node : sequence) {
            final SourceIndexLength sourceSection = node.getSourceIndexLength();

            if (sourceSection != null) {
                start = Integer.min(start, sourceSection.getCharIndex());
                end = Integer.max(end, sourceSection.getCharEnd());
            }
        }

        return new SourceIndexLength(start, end - start);
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

    protected RubyNode nilNode(Source source, SourceIndexLength sourceSection) {
        final RubyNode literal = new NilLiteralNode(false);
        literal.unsafeSetSourceSection(sourceSection);
        return literal;
    }

    protected RubyNode translateNodeOrNil(SourceIndexLength sourceSection, ParseNode node) {
        final RubyNode rubyNode;
        if (node == null || node instanceof NilImplicitParseNode) {
            rubyNode = nilNode(source, sourceSection);
        } else {
            rubyNode = node.accept(this);
        }
        return rubyNode;
    }

    public static RubyNode createCheckArityNode(Arity arity) {
        if (!arity.acceptsKeywords()) {
            return new CheckArityNode(arity);
        } else {
            return new CheckKeywordArityNode(arity);
        }
    }

    public SourceSection translateSourceSection(Source source, SourceIndexLength sourceSection) {
        if (sourceSection == null) {
            return null;
        } else {
            return sourceSection.toSourceSection(source);
        }
    }

    public static RubyNode loadSelf(RubyContext context, TranslatorEnvironment environment) {
        final FrameSlot slot = environment.getFrameDescriptor().findOrAddFrameSlot(SelfNode.SELF_IDENTIFIER);
        SourceIndexLength sourceSection = null;
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, new ProfileArgumentNode(new ReadSelfNode()));
    }

    public static <T extends RubyNode> T withSourceSection(SourceIndexLength sourceSection, T node) {
        if (sourceSection != null) {
            node.unsafeSetSourceSection(sourceSection);
        }
        return node;
    }

}
