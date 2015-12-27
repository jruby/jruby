/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

@NodeChildren({
        @NodeChild("block"),
        @NodeChild("self"),
        @NodeChild("blockArgument"),
        @NodeChild(value = "arguments", type = RubyNode[].class)
})
public abstract class CallBlockNode extends RubyNode {

    // Allowing the child to be Node.replace()'d.
    static class CallNodeWrapperNode extends Node {
        @Child DirectCallNode child;

        public CallNodeWrapperNode(DirectCallNode child) {
            this.child = insert(child);
        }
    }

    private final DeclarationContext declarationContext;

    public CallBlockNode(RubyContext context, SourceSection sourceSection, DeclarationContext declarationContext) {
        super(context, sourceSection);
        this.declarationContext = declarationContext;
    }

    public abstract Object executeCallBlock(VirtualFrame frame, DynamicObject block, Object self, DynamicObject blockArgument, Object[] arguments);

    @Specialization(
            guards = "getBlockCallTarget(block) == cachedCallTarget",
            limit = "getCacheLimit()")
    protected Object callBlock(VirtualFrame frame, DynamicObject block, Object self, Object blockArgument, Object[] arguments,
            @Cached("getBlockCallTarget(block)") CallTarget cachedCallTarget,
            @Cached("createBlockCallNode(cachedCallTarget)") CallNodeWrapperNode callNode) {
        final Object[] frameArguments = packArguments(block, self, blockArgument, arguments);
        return callNode.child.call(frame, frameArguments);
    }

    @Specialization
    protected Object callBlockUncached(VirtualFrame frame, DynamicObject block, Object self, Object blockArgument, Object[] arguments,
            @Cached("create()") IndirectCallNode callNode) {
        final Object[] frameArguments = packArguments(block, self, blockArgument, arguments);
        return callNode.call(frame, getBlockCallTarget(block), frameArguments);
    }

    private Object[] packArguments(DynamicObject block, Object self, Object blockArgument, Object[] arguments) {
        return RubyArguments.pack(
                Layouts.PROC.getMethod(block),
                Layouts.PROC.getDeclarationFrame(block),
                null,
                self,
                (DynamicObject) blockArgument,
                declarationContext,
                arguments);
    }

    protected static CallTarget getBlockCallTarget(DynamicObject block) {
        return Layouts.PROC.getCallTargetForType(block);
    }

    protected CallNodeWrapperNode createBlockCallNode(CallTarget callTarget) {
        final DirectCallNode callNode = Truffle.getRuntime().createDirectCallNode(callTarget);
        final CallNodeWrapperNode callNodeWrapperNode = new CallNodeWrapperNode(callNode);

        if (getContext().getOptions().YIELD_ALWAYS_CLONE && callNode.isCallTargetCloningAllowed()) {
            callNode.cloneCallTarget();
        }
        if (getContext().getOptions().YIELD_ALWAYS_INLINE && callNode.isInlinable()) {
            callNode.forceInlining();
        }
        return callNodeWrapperNode;
    }

    protected int getCacheLimit() {
        return getContext().getOptions().YIELD_CACHE;
    }

}
