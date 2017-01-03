/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

/**
 * Casts an object to a Ruby Proc object.
 */
@NodeChild("child")
public abstract class ToProcNode extends RubyNode {

    @Specialization(guards = "isNil(nil)")
    public DynamicObject doNil(Object nil) {
        return nil();
    }

    @Specialization(guards = "isRubyProc(proc)")
    public DynamicObject doRubyProc(DynamicObject proc) {
        return proc;
    }

    @Specialization(guards = "!isRubyProc(object)")
    public DynamicObject doObject(VirtualFrame frame, Object object,
            @Cached("createCallNode()") CallDispatchHeadNode toProc,
            @Cached("create()") BranchProfile errorProfile) {
        final Object coerced;
        try {
            coerced = toProc.call(frame, object, "to_proc");
        } catch (RaiseException e) {
            errorProfile.enter();
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getException()) == coreLibrary().getNoMethodErrorClass()) {
                throw new RaiseException(coreExceptions().typeErrorNoImplicitConversion(object, "Proc", this));
            } else {
                throw e;
            }
        }

        if (RubyGuards.isRubyProc(coerced)) {
            return (DynamicObject) coerced;
        } else {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorBadCoercion(object, "Proc", "to_proc", coerced, this));
        }
    }

    protected CallDispatchHeadNode createCallNode() {
        return DispatchHeadNodeFactory.createMethodCall();
    }

}
