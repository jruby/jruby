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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;

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

    @Specialization
    public RubyNilClass doNil(RubyNilClass nil) {
        return nil;
    }

    @Specialization
    public RubyProc doRubyProc(RubyProc proc) {
        return proc;
    }

    @Specialization
    public RubyProc doObject(VirtualFrame frame, RubyBasicObject object) {
        notDesignedForCompilation();

        return (RubyProc) toProc.call(frame, object, "to_proc", null);
    }

}
