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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeFactory;
import org.jruby.truffle.nodes.objects.TaintNode;
import org.jruby.truffle.nodes.objects.TaintNodeFactory;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class TaintResultNode extends RubyNode {

    private final boolean taintFromSelf;
    private final int[] taintFromParameters;
    private final ConditionProfile taintProfile = ConditionProfile.createBinaryProfile();

    @Child private RubyNode method;
    @Child private IsTaintedNode isTaintedNode;
    @Child private TaintNode taintNode;

    public TaintResultNode(boolean taintFromSelf, int[] taintFromParameters, RubyNode method) {
        super(method.getContext(), method.getEncapsulatingSourceSection());
        this.taintFromSelf = taintFromSelf;
        this.taintFromParameters = taintFromParameters;
        this.method = method;
        this.isTaintedNode = IsTaintedNodeFactory.create(getContext(), getSourceSection(), null);
    }

    public TaintResultNode(RubyContext context, SourceSection sourceSection, boolean taintFromSelf, int[] taintFromParameters) {
        super(context, sourceSection);
        this.taintFromSelf = taintFromSelf;
        this.taintFromParameters = taintFromParameters;
        this.isTaintedNode = IsTaintedNodeFactory.create(getContext(), getSourceSection(), null);
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

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        final RubyBasicObject result;

        try {
            result = method.executeRubyBasicObject(frame);
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(e);
        }

        if (result != getContext().getCoreLibrary().getNilObject()) {
            if (taintFromSelf) {
                maybeTaint((RubyBasicObject) RubyArguments.getSelf(frame.getArguments()), result);
            }

            // TODO (nirvdrum 05-Mar-15) If we never pass more than one value in practice, we should change the annotation to be int rather than int[].
            for (int i = 0; i < taintFromParameters.length; i++) {
                // It's possible the taintFromParamaters value was misconfigured by the user, but the far more likely
                // scenario is that the argument at that position is an UndefinedPlaceholder, which doesn't take up
                // a space in the frame.
                if (taintFromParameters[i] < RubyArguments.getUserArgumentsCount(frame.getArguments())) {
                    final RubyBasicObject taintSource =
                            (RubyBasicObject) RubyArguments.getUserArgument(frame.getArguments(), taintFromParameters[i]);
                    maybeTaint(taintSource, result);
                }
            }
        }

        return result;
    }
}
