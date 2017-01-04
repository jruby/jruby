/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.DispatchNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

// TODO(CS): copy and paste of ArrayCastNode

@NodeChild("child")
public abstract class HashCastNode extends RubyNode {

    @Child private CallDispatchHeadNode toHashNode = DispatchHeadNodeFactory.createMethodCall(MissingBehavior.RETURN_MISSING);

    protected abstract RubyNode getChild();

    @Specialization
    public DynamicObject cast(boolean value) {
        return nil();
    }

    @Specialization
    public DynamicObject cast(int value) {
        return nil();
    }

    @Specialization
    public DynamicObject cast(long value) {
        return nil();
    }

    @Specialization
    public DynamicObject cast(double value) {
        return nil();
    }

    @Specialization(guards = "isNil(nil)")
    public DynamicObject castNil(DynamicObject nil) {
        return nil();
    }

    @Specialization(guards = "isRubyBignum(value)")
    public DynamicObject castBignum(DynamicObject value) {
        return nil();
    }

    @Specialization(guards = "isRubyHash(hash)")
    public DynamicObject castHash(DynamicObject hash) {
        return hash;
    }

    @Specialization(guards = {"!isNil(object)", "!isRubyBignum(object)", "!isRubyHash(object)"})
    public Object cast(VirtualFrame frame, DynamicObject object,
            @Cached("create()") BranchProfile errorProfile) {
        final Object result = toHashNode.call(frame, object, "to_hash");

        if (result == DispatchNode.MISSING) {
            return nil();
        }

        if (!RubyGuards.isRubyHash(result)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorCantConvertTo(object, "Hash", "to_hash", result, this));
        }

        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        getChild().executeVoid(frame);
    }

}
