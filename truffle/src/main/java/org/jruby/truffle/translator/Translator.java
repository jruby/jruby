/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.lexer.yacc.InvalidSourcePosition;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.cli.Options;

import java.util.*;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    public static final Set<String> PRINT_AST_METHOD_NAMES = new HashSet<>(Arrays.asList(Options.TRUFFLE_TRANSLATOR_PRINT_AST.load().split(",")));
    public static final Set<String> PRINT_FULL_AST_METHOD_NAMES = new HashSet<>(Arrays.asList(Options.TRUFFLE_TRANSLATOR_PRINT_FULL_AST.load().split(",")));
    public static final Set<String> PRINT_PARSE_TREE_METHOD_NAMES = new HashSet<>(Arrays.asList(Options.TRUFFLE_TRANSLATOR_PRINT_PARSE_TREE.load().split(",")));

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

    protected abstract String getIdentifier();

}
