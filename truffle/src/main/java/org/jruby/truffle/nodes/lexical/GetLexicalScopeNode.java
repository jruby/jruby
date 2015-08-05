/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.lexical;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.GetCurrentMethodNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.InternalMethod;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class GetLexicalScopeNode extends RubyNode {

    public static GetLexicalScopeNode create(RubyContext context, SourceSection sourceSection) {
        return new GetLexicalScopeNode(context, sourceSection);
    }

    @Child GetCurrentMethodNode getCurrentMethodNode = new GetCurrentMethodNode(getContext(), getSourceSection());

    public GetLexicalScopeNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final InternalMethod method = getCurrentMethodNode.getCurrentMethod(frame);
        return method.getLexicalScope();
    }

    public LexicalScope getLexicalScope(VirtualFrame frame) {
        return (LexicalScope) execute(frame);
    }

}
