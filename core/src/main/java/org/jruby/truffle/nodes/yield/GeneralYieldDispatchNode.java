/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;

public class GeneralYieldDispatchNode extends YieldDispatchNode {

    @Child protected IndirectCallNode callNode;

    public GeneralYieldDispatchNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        final RubyMethod method = block.getMethod();
        return callNode.call(frame, method.getCallTarget(), RubyArguments.pack(method.getDeclarationFrame(), block.getSelfCapturedInScope(), block.getBlockCapturedInScope(), argumentsObjects));
    }

}
