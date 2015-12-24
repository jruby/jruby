/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ToSNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeGen;
import org.jruby.truffle.nodes.objects.TaintNode;
import org.jruby.truffle.nodes.objects.TaintNodeGen;
import org.jruby.truffle.runtime.RubyContext;

/**
 * A list of expressions to build up into a string.
 */
public final class InterpolatedStringNode extends RubyNode {

    @Children private final ToSNode[] children;

    @Child private CallDispatchHeadNode concatNode;
    @Child private CallDispatchHeadNode dupNode;
    @Child private IsTaintedNode isTaintedNode;
    @Child private TaintNode taintNode;

    private final ConditionProfile taintProfile = ConditionProfile.createCountingProfile();

    public InterpolatedStringNode(RubyContext context, SourceSection sourceSection, ToSNode[] children) {
        super(context, sourceSection);
        this.children = children;
        concatNode = DispatchHeadNodeFactory.createMethodCall(context);
        dupNode = DispatchHeadNodeFactory.createMethodCall(context);
        isTaintedNode = IsTaintedNodeGen.create(context, sourceSection, null);
        taintNode = TaintNodeGen.create(context, sourceSection, null);
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        final Object[] strings = new Object[children.length];

        boolean tainted = false;

        for (int n = 0; n < children.length; n++) {
            final Object toInterpolate = children[n].execute(frame);
            strings[n] = toInterpolate;
            tainted |= isTaintedNode.executeIsTainted(toInterpolate);
        }

        final Object string = concat(frame, strings);

        if (taintProfile.profile(tainted)) {
            taintNode.executeTaint(string);
        }

        return string;
    }

    private Object concat(VirtualFrame frame, Object[] strings) {
        // TODO(CS): there is a lot of copying going on here - and I think this is sometimes inner loop stuff

        Object builder = null;

        for (Object string : strings) {
            assert RubyGuards.isRubyString(string);

            if (builder == null) {
                builder = dupNode.call(frame, string, "dup", null);
            } else {
                builder = concatNode.call(frame, builder, "concat", null, string);
            }
        }

        return builder;
    }

}
