/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

/**
 * Define a method. That is, store the definition of a method and when executed
 * produce the executable object that results.
 */
public class MethodDefinitionNode extends RubyNode {

    private final String name;
    private final SharedMethodInfo sharedMethodInfo;

    private final CallTarget callTarget;

    private final boolean requiresDeclarationFrame;

    public MethodDefinitionNode(RubyContext context, SourceSection sourceSection, String name, SharedMethodInfo sharedMethodInfo,
            boolean requiresDeclarationFrame, CallTarget callTarget) {
        super(context, sourceSection);
        this.name = name;
        this.sharedMethodInfo = sharedMethodInfo;
        this.requiresDeclarationFrame = requiresDeclarationFrame;
        this.callTarget = callTarget;
    }

    public RubyMethod executeMethod(VirtualFrame frame) {
        notDesignedForCompilation();

        final MaterializedFrame declarationFrame;

        if (requiresDeclarationFrame) {
            declarationFrame = frame.materialize();
        } else {
            declarationFrame = null;
        }

        return executeMethod(frame, declarationFrame);
    }

    public RubyMethod executeMethod(VirtualFrame frame, MaterializedFrame declarationFrame) {
        notDesignedForCompilation();

        return new RubyMethod(sharedMethodInfo, name, null, null, false, callTarget, declarationFrame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeMethod(frame);
    }

    public String getName() {
        return name;
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }
}
