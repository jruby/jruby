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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    protected final RubyContext context;
    protected final Source source;
    private final String sourceIdentifier;

    public Translator(RubyContext context, Source source, String sourceIdentifier) {
        this.context = context;
        this.source = source;
        this.sourceIdentifier = sourceIdentifier;
    }

    protected SourceSection translate(org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        return translate(source, sourceIdentifier, sourcePosition);
    }

    public static SourceSection translate(Source source, String sourceIdentifier, org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        try {
            if (sourcePosition.getStartLine() == -1) {
                // TODO(CS): why on earth is the line -1?
                return source.createSection(sourceIdentifier, 0, source.getCode().length());
            } else {
                // TODO(CS): can we not get column info?
                return source.createSection(sourceIdentifier, sourcePosition.getStartLine() + 1);
            }
        } catch (UnsupportedOperationException e) {
            return source.createSection(sourceIdentifier, 0, source.getCode().length());
        }
    }

}
