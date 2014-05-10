/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

/**
 * The root node in an AST for a method. Unlike {@link RubyNode}, this has a single entry point,
 * {@link #execute}, which Truffle knows about and can create a {@link CallTarget} from.
 */
public class RubyRootNode extends RootNode {

    private final SharedMethodInfo sharedMethodInfo;
    @Child protected RubyNode body;
    private final RubyNode uninitializedBody;


    public RubyRootNode(SourceSection sourceSection, FrameDescriptor frameDescriptor, SharedMethodInfo sharedMethodInfo, RubyNode body) {
        super(sourceSection, frameDescriptor);
        assert body != null;
        this.body = body;
        this.sharedMethodInfo = sharedMethodInfo;
        uninitializedBody = NodeUtil.cloneNode(body);
    }

    public RubyRootNode cloneRubyRootNode() {
        return new RubyRootNode(getSourceSection(), getFrameDescriptor(), sharedMethodInfo, NodeUtil.cloneNode(uninitializedBody));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    @Override
    public RootNode split() {
        return cloneRubyRootNode();
    }

    @Override
    public boolean isSplittable() {
        return true;
    }

    public void reportLoopCountThroughBlocks(int count) {
        CompilerAsserts.neverPartOfCompilation();

        for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
            final RootNode rootNode = frame.getCallNode().getRootNode();

            rootNode.reportLoopCount(count);

            if (rootNode instanceof RubyRootNode && !((RubyRootNode) rootNode).getSharedMethodInfo().isBlock()) {
                break;
            }
        }

        reportLoopCount(count);
    }

    @Override
    public String toString() {
        return sharedMethodInfo.toString();
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }

}
