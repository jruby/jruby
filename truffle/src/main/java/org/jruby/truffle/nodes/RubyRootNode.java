/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.ExecutionContext;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyLanguage;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

/**
 * The root node in an AST for a method. Unlike {@link RubyNode}, this has a single entry point,
 * {@link #execute}, which Truffle knows about and can create a {@link CallTarget} from.
 */
public class RubyRootNode extends RootNode {

    private final RubyContext context;
    private final SharedMethodInfo sharedMethodInfo;
    @Child private RubyNode body;
    private final boolean needsDeclarationFrame;

    public RubyRootNode(RubyContext context, SourceSection sourceSection, FrameDescriptor frameDescriptor, SharedMethodInfo sharedMethodInfo, RubyNode body) {
        this(context, sourceSection, frameDescriptor, sharedMethodInfo, body, false);
    }

    public RubyRootNode(RubyContext context, SourceSection sourceSection, FrameDescriptor frameDescriptor, SharedMethodInfo sharedMethodInfo, RubyNode body, boolean needsDeclarationFrame) {
        super(RubyLanguage.class, sourceSection, frameDescriptor);
        assert body != null;
        this.context = context;
        this.body = body;
        this.sharedMethodInfo = sharedMethodInfo;
        this.needsDeclarationFrame = needsDeclarationFrame;
    }

    public RubyRootNode withBody(RubyNode body) {
        return new RubyRootNode(context, getSourceSection(), getFrameDescriptor(), sharedMethodInfo, body, needsDeclarationFrame);
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        context.getSafepointManager().poll(this);
        return body.execute(frame);
    }

    @Override
    public String toString() {
        return sharedMethodInfo.toString();
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }

    public RubyNode getBody() {
        return body;
    }

    @Override
    public ExecutionContext getExecutionContext() {
        return context;
    }

    public boolean needsDeclarationFrame() {
        return needsDeclarationFrame;
    }

}
