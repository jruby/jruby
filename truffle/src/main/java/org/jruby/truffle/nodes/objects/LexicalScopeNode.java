/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;

/**
 * Find the RubyModule enclosing us lexically.
 */
public class LexicalScopeNode extends RubyNode {

    final LexicalScope lexicalScope;

    public LexicalScopeNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope) {
        super(context, sourceSection);
        this.lexicalScope = lexicalScope;
    }

    @Override
    public RubyModule executeRubyModule(VirtualFrame frame) {
        return lexicalScope.getLiveModule();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyModule(frame);
    }

}
