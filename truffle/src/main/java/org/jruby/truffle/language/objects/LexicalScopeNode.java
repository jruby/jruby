/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;

public class LexicalScopeNode extends RubyNode {

    private final LexicalScope lexicalScope;

    public LexicalScopeNode(LexicalScope lexicalScope) {
        this.lexicalScope = lexicalScope;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return lexicalScope.getLiveModule();
    }

}
