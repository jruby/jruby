/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

public class TaintResultNode extends RubyNode {

    private final boolean needsSelf;
    private final int taintSourceIndex;
    private final ConditionProfile taintProfile = ConditionProfile.createBinaryProfile();

    @Child private RubyNode method;
    @Child private KernelNodes.KernelIsTaintedNode isTaintedNode;
    @Child private KernelNodes.KernelTaintNode taintNode;

    public TaintResultNode(RubyContext context, SourceSection sourceSection, boolean needSelf, int taintSourceIndex, RubyNode method) {
        super(context, sourceSection);
        this.needsSelf = needSelf;
        this.taintSourceIndex = taintSourceIndex;
        this.method = method;
        this.isTaintedNode = KernelNodesFactory.KernelIsTaintedNodeFactory.create(context, sourceSection, new RubyNode[]{});
    }

    public TaintResultNode(TaintResultNode prev) {
        super(prev);
        needsSelf = prev.needsSelf;
        taintSourceIndex = prev.taintSourceIndex;
        method = prev.method;
        isTaintedNode = prev.isTaintedNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object result = method.execute(frame);

        if (result != getContext().getCoreLibrary().getNilObject()) {
            final Object taintSource;

            if (needsSelf && taintSourceIndex == 0) {
                taintSource = RubyArguments.getSelf(frame.getArguments());
            } else {
                final int adjustedIndex = needsSelf ? taintSourceIndex - 1 : taintSourceIndex;
                taintSource = RubyArguments.getUserArgument(frame.getArguments(), adjustedIndex);
            }

            if (taintProfile.profile(isTaintedNode.isTainted(taintSource))) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(KernelNodesFactory.KernelTaintNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
                }

                taintNode.taint(result);
            }
        }

        return result;
    }
}
