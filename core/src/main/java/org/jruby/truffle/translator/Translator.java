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

import com.oracle.truffle.api.Source;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.impl.DefaultSourceSection;
import org.jruby.lexer.yacc.IDetailedSourcePosition;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    protected final RubyContext context;
    protected final Source source;

    protected SourceSection parentSourceSection;

    public Translator(RubyContext context, Source source) {
        this.context = context;
        this.source = source;
    }

    protected SourceSection translate(org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        if (sourcePosition == ISourcePosition.INVALID_POSITION) {
            if (parentSourceSection == null) {
                throw new UnsupportedOperationException("Truffle doesn't want invalid positions - find a way to give me a real position!");
            } else {
                return parentSourceSection;
            }
        }

        if (sourcePosition instanceof IDetailedSourcePosition) {
            final IDetailedSourcePosition detailedSourcePosition = (IDetailedSourcePosition) sourcePosition;
            return new DefaultSourceSection(source, "(identifier)", sourcePosition.getStartLine() + 1, 0, detailedSourcePosition.getOffset(), detailedSourcePosition.getLength());
        } else if (RubyContext.ALLOW_SIMPLE_SOURCE_SECTIONS) {
            // If we didn't run with -X+T, so maybe we're using truffelize, we might still get simple source sections
            return new DefaultSourceSection(source, "(identifier)", sourcePosition.getStartLine() + 1, 0, 0, 0);
        } else {
            throw new UnsupportedOperationException("Truffle needs detailed source positions unless you know what you are doing and set truffle.allow_simple_source_sections - got " + sourcePosition.getClass());
        }

    }
}
