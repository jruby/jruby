/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.methods.CallBoundMethodNode;
import org.jruby.truffle.language.methods.CallBoundMethodNodeGen;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.yield.CallBlockNode;
import org.jruby.truffle.language.yield.CallBlockNodeGen;

@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("arguments")
})
abstract class ForeignExecuteHelperNode extends RubyNode {

    public abstract Object executeCall(VirtualFrame frame, Object receiver, Object[] arguments);

    @Specialization(guards = "isRubyProc(proc)")
    protected Object callProc(VirtualFrame frame, DynamicObject proc, Object[] arguments,
                              @Cached("createCallBlockNode()") CallBlockNode callBlockNode) {
        Object self = Layouts.PROC.getSelf(proc);
        return callBlockNode.executeCallBlock(frame, proc, self, null, arguments);
    }

    protected CallBlockNode createCallBlockNode() {
        return CallBlockNodeGen.create(DeclarationContext.BLOCK, null, null, null, null);
    }

    @Specialization(guards = "isRubyMethod(method)")
    protected Object callMethod(VirtualFrame frame, DynamicObject method, Object[] arguments,
                                @Cached("createCallBoundMethodNode()") CallBoundMethodNode callBoundMethodNode) {
        return callBoundMethodNode.executeCallBoundMethod(frame, method, arguments, nil());
    }

    protected CallBoundMethodNode createCallBoundMethodNode() {
        return CallBoundMethodNodeGen.create(null, null, null);
    }

}
