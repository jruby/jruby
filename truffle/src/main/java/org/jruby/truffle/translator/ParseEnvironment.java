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

import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Translator environment, unique per parse/translation.
 */
public class ParseEnvironment {

    private LexicalScope lexicalScope;

    public ParseEnvironment(RubyContext context) {
        lexicalScope = context.getRootLexicalScope();
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

}
