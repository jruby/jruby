/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyNilClass;

// TODO(CS): copy and paste of ArrayCastNode

@NodeChild("child")
public abstract class HashCastNode extends RubyNode {

    @Child private CallDispatchHeadNode toHashNode;

    public HashCastNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        toHashNode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
    }

    public HashCastNode(HashCastNode prev) {
        super(prev);
        toHashNode = prev.toHashNode;
    }

    protected abstract RubyNode getChild();

    @Specialization
    public RubyNilClass cast(boolean value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(int value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(long value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(double value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(RubyBignum value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyHash cast(RubyHash hash) {
        return hash;
    }

    @Specialization
    public RubyNilClass cast(RubyNilClass nil) {
        return nil;
    }

    @Specialization(guards = {"!isRubyNilClass", "!isRubyHash"})
    public Object cast(VirtualFrame frame, RubyBasicObject object) {
        notDesignedForCompilation();

        final Object result = toHashNode.call(frame, object, "to_hash", null, new Object[]{});

        if (result == DispatchNode.MISSING) {
            return getContext().getCoreLibrary().getNilObject();
        }

        if (!(result instanceof RubyHash)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorShouldReturn(object.toString(), "to_hash", "HAsh", this));
        }

        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        getChild().executeVoid(frame);
    }

}
