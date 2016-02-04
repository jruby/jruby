/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.yield;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

public class YieldDispatchHeadNode extends Node {

    @Child CallBlockNode callBlockNode;

    public YieldDispatchHeadNode(RubyContext context) {
        this(context, DeclarationContext.BLOCK);
    }

    public YieldDispatchHeadNode(RubyContext context, DeclarationContext declarationContext) {
        callBlockNode = CallBlockNodeGen.create(context, null, declarationContext, null, null, null, null);
    }

    public Object dispatch(VirtualFrame frame, DynamicObject block, Object... argumentsObjects) {
        assert block == null || RubyGuards.isRubyProc(block);
        return callBlockNode.executeCallBlock(frame, block, Layouts.PROC.getSelf(block), Layouts.PROC.getBlock(block), argumentsObjects);
    }

    public Object dispatchWithModifiedBlock(VirtualFrame frame, DynamicObject block, DynamicObject modifiedBlock, Object... argumentsObjects) {
        assert block == null || RubyGuards.isRubyProc(block);
        assert modifiedBlock == null || RubyGuards.isRubyProc(modifiedBlock);
        return callBlockNode.executeCallBlock(frame, block, Layouts.PROC.getSelf(block), modifiedBlock, argumentsObjects);
    }

    public Object dispatchWithModifiedSelf(VirtualFrame currentFrame, DynamicObject block, Object self, Object... argumentsObjects) {
        assert block == null || RubyGuards.isRubyProc(block);
        return callBlockNode.executeCallBlock(currentFrame, block, self, Layouts.PROC.getBlock(block), argumentsObjects);
    }

}
