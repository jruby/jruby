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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public abstract class Translator extends org.jruby.ast.visitor.AbstractNodeVisitor<RubyNode> {

    protected final RubyContext context;
    protected final Source source;

    public Translator(RubyContext context, Source source) {
        this.context = context;
        this.source = source;
    }

    protected SourceSection translate(org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        try {
            // TODO(cs): get an identifier
            final String identifier = "(identifier)";

            // TODO(cs): work out the start column
            final int startColumn = -1;

            final int charLength = -1;

            return new DefaultSourceSection(source, identifier, sourcePosition.getStartLine() + 1, startColumn, -1, charLength);
        } catch (UnsupportedOperationException e) {
            // In some circumstances JRuby can't tell you what the position is
            return translate(new org.jruby.lexer.yacc.SimpleSourcePosition("(unknown)", 0));
        }
    }

}
