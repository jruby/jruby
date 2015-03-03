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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeFactory;
import org.jruby.truffle.nodes.objects.TaintNode;
import org.jruby.truffle.nodes.objects.TaintNodeFactory;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class TaintResultNode extends RubyNode {

    private final boolean needsSelf;
    private final int taintSourceIndex;
    private final ConditionProfile taintProfile = ConditionProfile.createBinaryProfile();

    @Child private RubyNode method;
    @Child private IsTaintedNode isTaintedNode;
    @Child private TaintNode taintNode;

    public TaintResultNode(RubyContext context, SourceSection sourceSection, boolean needSelf, int taintSourceIndex, RubyNode method) {
        super(context, sourceSection);
        this.needsSelf = needSelf;
        this.taintSourceIndex = taintSourceIndex;
        this.method = method;
        this.isTaintedNode = IsTaintedNodeFactory.create(context, sourceSection, null);
    }

    public TaintResultNode(TaintResultNode prev) {
        super(prev);
        needsSelf = prev.needsSelf;
        taintSourceIndex = prev.taintSourceIndex;
        method = prev.method;
        isTaintedNode = prev.isTaintedNode;
    }

    public Object maybeTaint(RubyBasicObject source, RubyBasicObject result) {
        if (taintProfile.profile(isTaintedNode.isTainted(source))) {
            if (taintNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintNode = insert(TaintNodeFactory.create(getContext(), getSourceSection(), null));
            }

            taintNode.taint(result);
        }

        return result;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyBasicObject result;

        try {
            result = method.executeRubyBasicObject(frame);
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(e);
        }

        if (result != getContext().getCoreLibrary().getNilObject()) {
            final RubyBasicObject taintSource;

            if (needsSelf && taintSourceIndex == 0) {
                taintSource = (RubyBasicObject) RubyArguments.getSelf(frame.getArguments());
            } else {
                final int adjustedIndex = needsSelf ? taintSourceIndex - 1 : taintSourceIndex;
                taintSource = (RubyBasicObject) RubyArguments.getUserArgument(frame.getArguments(), adjustedIndex);
            }

            maybeTaint(taintSource, result);
        }

        return result;
    }
}
