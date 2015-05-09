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
import org.jruby.truffle.translator.TranslatorEnvironment.BlockID;

/**
 * Translator environment, unique per parse/translation.
 */
public class ParseEnvironment {

    private LexicalScope lexicalScope;
    private long nextReturnID;

    public ParseEnvironment(RubyContext context) {
        lexicalScope = context.getRootLexicalScope();
        nextReturnID = 0;
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

    public long allocateReturnID() {
        if (nextReturnID == Long.MAX_VALUE) {
            throw new RuntimeException("Return IDs exhausted");
        }

        final long allocated = nextReturnID;
        nextReturnID++;
        return allocated;
    }

    public BlockID allocateBlockID() {
        return new BlockID();
    }

}
