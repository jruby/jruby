/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.lexical;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Find the RubyModule enclosing us lexically.
 */
public class GetLexicalScopeModuleNode extends RubyNode {

    public GetLexicalScopeModuleNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyBasicObject(frame);
    }

    @Override
    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) {
        final InternalMethod method = RubyArguments.getMethod(frame.getArguments());
        return method.getLexicalScope().getModule();
    }

}
