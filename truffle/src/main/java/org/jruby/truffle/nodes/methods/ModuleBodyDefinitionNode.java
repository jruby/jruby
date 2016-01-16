/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

/**
 * Define a method from a module body (module/class/class << self ... end).
 */
public class ModuleBodyDefinitionNode extends RubyNode {

    private final String name;
    private final SharedMethodInfo sharedMethodInfo;
    private final CallTarget callTarget;
    private final boolean captureBlock;

    public ModuleBodyDefinitionNode(RubyContext context, SourceSection sourceSection, String name, SharedMethodInfo sharedMethodInfo,
            CallTarget callTarget, boolean captureBlock) {
        super(context, sourceSection);
        this.name = name;
        this.sharedMethodInfo = sharedMethodInfo;
        this.callTarget = callTarget;
        this.captureBlock = captureBlock;
    }

    public InternalMethod executeMethod(VirtualFrame frame) {
        final DynamicObject dummyModule = getContext().getCoreLibrary().getObjectClass();
        final Visibility dummyVisibility = Visibility.PUBLIC;

        final DynamicObject capturedBlock;

        if (captureBlock) {
            capturedBlock = RubyArguments.getBlock(frame.getArguments());
        } else {
            capturedBlock = null;
        }

        return new InternalMethod(sharedMethodInfo, name, dummyModule, dummyVisibility, false, null, callTarget, capturedBlock, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeMethod(frame);
    }

}
