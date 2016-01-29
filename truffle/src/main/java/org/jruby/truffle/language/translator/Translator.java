/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.translator;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.lexer.yacc.InvalidSourcePosition;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.language.literal.NilNode;
import org.jruby.truffle.runtime.RubyContext;

import java.util.*;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    public static final Set<String> ALWAYS_DEFINED_GLOBALS = new HashSet<>(Collections.singletonList("$~"));
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

    protected SourceSection translate(org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        return translate(source, sourcePosition, getIdentifier());
    }

    protected SourceSection translate(org.jruby.lexer.yacc.ISourcePosition sourcePosition, String identifier) {
        return translate(source, sourcePosition, identifier);
    }

    private SourceSection translate(Source source, org.jruby.lexer.yacc.ISourcePosition sourcePosition, String identifier) {
        if (sourcePosition == InvalidSourcePosition.INSTANCE) {
            if (parentSourceSection.peek() == null) {
                throw new UnsupportedOperationException("Truffle doesn't want invalid positions - find a way to give me a real position!");
            } else {
                return parentSourceSection.peek();
            }
        } else {
            return source.createSection(identifier, sourcePosition.getLine() + 1);
        }
    }

    protected RubyNode nilNode(SourceSection sourceSection) {
        return new NilNode(context, sourceSection);
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

    protected abstract String getIdentifier();

}
