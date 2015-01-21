/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.lexer.yacc.DetailedSourcePosition;
import org.jruby.lexer.yacc.InvalidSourcePosition;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.cli.Options;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    public static final Set<String> PRINT_AST_METHOD_NAMES = new HashSet<>(Arrays.asList(Options.TRUFFLE_TRANSLATOR_PRINT_AST.load().split(",")));
    public static final Set<String> PRINT_FULL_AST_METHOD_NAMES = new HashSet<>(Arrays.asList(Options.TRUFFLE_TRANSLATOR_PRINT_FULL_AST.load().split(",")));
    public static final Set<String> PRINT_PARSE_TREE_METHOD_NAMES = new HashSet<>(Arrays.asList(Options.TRUFFLE_TRANSLATOR_PRINT_PARSE_TREE.load().split(",")));

    protected final RubyNode currentNode;
    protected final RubyContext context;
    protected final Source source;

    protected SourceSection parentSourceSection;

    public Translator(RubyNode currentNode, RubyContext context, Source source) {
        this.currentNode = currentNode;
        this.context = context;
        this.source = source;
    }

    protected SourceSection translate(org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        return translate(source, sourcePosition);
    }

    public SourceSection translate(Source source, org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        if (sourcePosition == InvalidSourcePosition.INSTANCE) {
            if (parentSourceSection == null) {
                throw new UnsupportedOperationException("Truffle doesn't want invalid positions - find a way to give me a real position!");
            } else {
                return parentSourceSection;
            }
        } else if (sourcePosition instanceof DetailedSourcePosition) {
            final DetailedSourcePosition detailedSourcePosition = (DetailedSourcePosition) sourcePosition;

            try {
                return source.createSection(getIdentifier(), detailedSourcePosition.getOffset(), detailedSourcePosition.getLength());
            } catch (IllegalArgumentException e) {
                // In some cases we still get bad offsets with the detailed source positions
                return source.createSection(getIdentifier(), sourcePosition.getLine() + 1);
            }
        } else if (Options.TRUFFLE_ALLOW_SIMPLE_SOURCE_SECTIONS.load()) {
            return source.createSection(getIdentifier(), sourcePosition.getLine() + 1);
        } else {
            throw new UnsupportedOperationException("Truffle needs detailed source positions unless you know what you are doing and set truffle.allow_simple_source_sections - got " + sourcePosition.getClass());
        }
    }

    protected abstract String getIdentifier();

}
