/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * Define a block. That is, store the definition of a block and when executed produce the executable
 * object that results.
 */
@NodeInfo(shortName = "block-def")
public class BlockDefinitionNode extends MethodDefinitionNode {

    public BlockDefinitionNode(RubyContext context, SourceSection sourceSection, String name, UniqueMethodIdentifier uniqueIdentifier,
                    boolean requiresDeclarationFrame, CallTarget callTarget) {
        super(context, sourceSection, name, uniqueIdentifier, requiresDeclarationFrame, callTarget, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyContext context = getContext();

        final MaterializedFrame declarationFrame;

        if (requiresDeclarationFrame) {
            declarationFrame = frame.materialize();
        } else {
            declarationFrame = null;
        }

        final RubyArguments arguments = new RubyArguments(frame.getArguments());

        final RubyMethod method = new RubyMethod(getSourceSection(), uniqueIdentifier, name, null, Visibility.PUBLIC, false, false, true, callTarget, declarationFrame);

        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, arguments.getSelf(), arguments.getBlock(), method);
    }

}
