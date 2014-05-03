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
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * Define a block. That is, store the definition of a block and when executed produce the executable
 * object that results.
 */
@NodeInfo(shortName = "block-def")
public class BlockDefinitionNode extends MethodDefinitionNode {

    public BlockDefinitionNode(RubyContext context, SourceSection sourceSection, String name, SharedMethodInfo methodInfo,
                    boolean requiresDeclarationFrame, RubyRootNode rootNode) {
        super(context, sourceSection, name, methodInfo, requiresDeclarationFrame, rootNode, false);
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

        final RubyRootNode rootNodeClone = NodeUtil.cloneNode(rootNode);
        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNodeClone);
        final RubyMethod method = new RubyMethod(sharedMethodInfo, name, null, Visibility.PUBLIC, false, callTarget, declarationFrame);
        rootNodeClone.setMethod(method);

        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, RubyArguments.getSelf(frame.getArguments()), RubyArguments.getBlock(frame.getArguments()), method);
    }

}
