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

import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.control.BreakID;
import org.jruby.truffle.language.control.ReturnID;

/**
 * Translator environment, unique per parse/translation.
 */
public class ParseEnvironment {

    private LexicalScope lexicalScope = null;

    public ParseEnvironment(RubyContext context) {
    }

    public void resetLexicalScope(LexicalScope lexicalScope) {
        this.lexicalScope = lexicalScope;
    }

    public LexicalScope getLexicalScope() {
        return lexicalScope;
    }

    public LexicalScope pushLexicalScope() {
        return lexicalScope = new LexicalScope(lexicalScope);
    }

    public void popLexicalScope() {
        lexicalScope = lexicalScope.getParent();
    }

    public ReturnID allocateReturnID() {
        return new ReturnID();
    }

    public BreakID allocateBreakID() {
        return new BreakID();
    }

}
