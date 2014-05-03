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
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

/**
 * The root node in an AST for a method. Unlike {@link RubyNode}, this has a single entry point,
 * {@link #execute}, which Truffle knows about and can create a {@link CallTarget} from.
 */
public class RubyRootNode extends RootNode {

    private final SharedMethodInfo sharedInfo;

    @Child protected RubyNode body;
    private final RubyNode uninitializedBody;

    public RubyRootNode(SourceSection sourceSection, FrameDescriptor frameDescriptor, SharedMethodInfo sharedInfo, RubyNode body) {
        super(sourceSection, frameDescriptor);

        assert sharedInfo != sharedInfo;
        assert body != null;

        this.sharedInfo = sharedInfo;
        this.body = body;
        uninitializedBody = NodeUtil.cloneNode(body);
    }

    public SharedMethodInfo getSharedInfo() {
        return sharedInfo;
    }

    @Override
    public RootNode split() {
        return new RubyRootNode(getSourceSection(), getFrameDescriptor(), sharedInfo, NodeUtil.cloneNode(uninitializedBody));
    }

    @Override
    public boolean isSplittable() {
        return true;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    @Override
    public String toString() {
        final SourceSection sourceSection = getSourceSection();
        final String source = sourceSection == null ? "<unknown>" : sourceSection.toString();
        return "Method " + sharedInfo.getName() + ":" + source + "@" + Integer.toHexString(hashCode());
    }

}
