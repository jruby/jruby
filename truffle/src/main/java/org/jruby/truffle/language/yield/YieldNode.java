/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.yield;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.methods.DeclarationContext;

public class YieldNode extends Node {

    private final RubyContext context;
    private final DeclarationContext declarationContext;

    @Child private CallBlockNode callBlockNode;

    public YieldNode(RubyContext context) {
        this(context, DeclarationContext.BLOCK);
    }

    public YieldNode(RubyContext context, DeclarationContext declarationContext) {
        this.context = context;
        this.declarationContext = declarationContext;
    }

    public Object dispatch(VirtualFrame frame,
                           DynamicObject block,
                           Object... argumentsObjects) {
        return getCallBlockNode().executeCallBlock(
                frame,
                block,
                Layouts.PROC.getSelf(block),
                Layouts.PROC.getBlock(block),
                argumentsObjects);
    }

    public Object dispatchWithModifiedBlock(VirtualFrame frame,
                                            DynamicObject block,
                                            DynamicObject modifiedBlock,
                                            Object... argumentsObjects) {
        return getCallBlockNode().executeCallBlock(
                frame,
                block,
                Layouts.PROC.getSelf(block),
                modifiedBlock,
                argumentsObjects);
    }

    public Object dispatchWithModifiedSelf(VirtualFrame currentFrame,
                                           DynamicObject block,
                                           Object self,
                                           Object... argumentsObjects) {
        return getCallBlockNode().executeCallBlock(
                currentFrame,
                block,
                self,
                Layouts.PROC.getBlock(block),
                argumentsObjects);
    }

    private CallBlockNode getCallBlockNode() {
        if (callBlockNode == null) {
            CompilerDirectives.transferToInterpreter();
            callBlockNode = insert(CallBlockNodeGen.create(context, null, declarationContext, null, null, null, null));
        }

        return callBlockNode;
    }

}
