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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ToSNode;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeFactory;
import org.jruby.truffle.nodes.objects.TaintNode;
import org.jruby.truffle.nodes.objects.TaintNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyString;

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
        isTaintedNode = IsTaintedNodeFactory.create(context, sourceSection, null);
        taintNode = TaintNodeFactory.create(context, sourceSection, null);
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        final RubyString[] strings = new RubyString[children.length];

        boolean tainted = false;

        for (int n = 0; n < children.length; n++) {
            final RubyString toInterpolate = children[n].executeRubyString(frame);
            strings[n] = toInterpolate;
            tainted |= isTaintedNode.isTainted(toInterpolate);
        }

        final RubyString string =  concat(strings);

        if (taintProfile.profile(tainted)) {
            taintNode.taint(string);
        }

        return string;
    }

    @CompilerDirectives.TruffleBoundary
    private RubyString concat(RubyString[] strings) {
        // TODO(CS): there is a lot of copying going on here - and I think this is sometimes inner loop stuff

        org.jruby.RubyString builder = null;

        for (RubyString string : strings) {
            if (builder == null) {
                builder = getContext().toJRuby(string);
            } else {
                try {
                    builder.append19(getContext().toJRuby(string));
                } catch (org.jruby.exceptions.RaiseException e) {
                    throw new RaiseException(getContext().getCoreLibrary().encodingCompatibilityErrorIncompatible(builder.getEncoding().getCharsetName(), string.getBytes().getEncoding().getCharsetName(), this));
                }
            }
        }

        return getContext().toTruffle(builder);
    }

}
