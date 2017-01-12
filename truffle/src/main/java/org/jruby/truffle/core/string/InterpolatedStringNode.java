/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.core.cast.ToSNode;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.IsTaintedNode;
import org.jruby.truffle.language.objects.TaintNode;

/**
 * A list of expressions to build up into a string.
 */
public final class InterpolatedStringNode extends RubyNode {

    @Children private final ToSNode[] children;

    @Child private StringNodes.StringAppendPrimitiveNode appendNode = StringNodesFactory.StringAppendPrimitiveNodeFactory.create(new RubyNode[] {});
    @Child private CallDispatchHeadNode dupNode = DispatchHeadNodeFactory.createMethodCall();
    @Child private IsTaintedNode isTaintedNode = IsTaintedNode.create();
    @Child private TaintNode taintNode = TaintNode.create();

    private final ConditionProfile taintProfile = ConditionProfile.createCountingProfile();

    public InterpolatedStringNode(ToSNode[] children) {
        this.children = children;
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

        DynamicObject builder = null;

        // TODO (nirvdrum 11-Jan-16) Rewrite to avoid massively unbalanced trees.
        for (Object string : strings) {
            assert RubyGuards.isRubyString(string);

            if (builder == null) {
                builder = (DynamicObject) dupNode.call(frame, string, "dup");
            } else {
                builder = appendNode.executeStringAppend(builder, (DynamicObject) string);
            }
        }

        return builder;
    }

}
