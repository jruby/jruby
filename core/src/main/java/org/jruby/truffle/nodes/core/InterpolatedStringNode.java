/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.ToSNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

/**
 * A list of expressions to build up into a string.
 */
public final class InterpolatedStringNode extends RubyNode {

    @Children protected final ToSNode[] children;

    @Child protected KernelNodes.TaintedNode taintedNode;
    @Child protected KernelNodes.TaintNode taintNode;

    private final ConditionProfile taintProfile = ConditionProfile.createCountingProfile();

    public InterpolatedStringNode(RubyContext context, SourceSection sourceSection, ToSNode[] children) {
        super(context, sourceSection);
        this.children = children;
        taintedNode = KernelNodesFactory.TaintedNodeFactory.create(context, sourceSection, new RubyNode[]{});
        taintNode = KernelNodesFactory.TaintNodeFactory.create(context, sourceSection, new RubyNode[]{});
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        final RubyString[] strings = new RubyString[children.length];

        boolean tainted = false;

        for (int n = 0; n < children.length; n++) {
            final RubyString toInterpolate = children[n].executeString(frame);
            strings[n] = toInterpolate;
            tainted |= taintedNode.tainted(toInterpolate);
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

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (int n = 0; n < children.length; n++) {
            children[n].executeVoid(frame);
        }
    }

}
