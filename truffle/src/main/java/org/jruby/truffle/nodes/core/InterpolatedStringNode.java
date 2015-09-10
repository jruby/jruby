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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ToSNode;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeGen;
import org.jruby.truffle.nodes.objects.TaintNode;
import org.jruby.truffle.nodes.objects.TaintNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;

/**
 * A list of expressions to build up into a string.
 */
public final class InterpolatedStringNode extends RubyNode {

    @Children private final ToSNode[] children;

    @Child private IsTaintedNode isTaintedNode;
    @Child private TaintNode taintNode;

    private final ConditionProfile taintProfile = ConditionProfile.createCountingProfile();

    public InterpolatedStringNode(RubyContext context, SourceSection sourceSection, ToSNode[] children) {
        super(context, sourceSection);
        this.children = children;
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

        final Object string = concat(strings);

        if (taintProfile.profile(tainted)) {
            taintNode.executeTaint(string);
        }

        return string;
    }

    @TruffleBoundary
    private DynamicObject concat(Object[] strings) {
        // TODO(CS): there is a lot of copying going on here - and I think this is sometimes inner loop stuff

        org.jruby.RubyString builder = null;

        for (Object string : strings) {
            assert RubyGuards.isRubyString(string);

            if (builder == null) {
                builder = getContext().toJRubyString((DynamicObject) string);
            } else {
                try {
                    builder.append19(getContext().toJRuby(string));
                } catch (org.jruby.exceptions.RaiseException e) {
                    throw new RaiseException(getContext().getCoreLibrary().encodingCompatibilityErrorIncompatible(builder.getEncoding().getCharsetName(), Layouts.STRING.getByteList((DynamicObject) string).getEncoding().getCharsetName(), this));
                }
            }
        }

        return getContext().toTruffle(builder);
    }

}
