/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Casts an object to a Ruby Proc object.
 */
@NodeChild("child")
public abstract class ProcCastNode extends RubyNode {

    @Child private CallDispatchHeadNode toProc;

    public ProcCastNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        toProc = DispatchHeadNodeFactory.createMethodCall(context);
    }

    @Specialization(guards = "isNil(nil)")
    public DynamicObject doNil(Object nil) {
        return nil();
    }

    @Specialization(guards = "isRubyProc(proc)")
    public DynamicObject doRubyProc(DynamicObject proc) {
        return proc;
    }

    @Specialization
    public Object doObject(VirtualFrame frame, DynamicObject object) {
        return toProc.call(frame, object, "to_proc", null);
    }

}
