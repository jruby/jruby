/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.kernel.TraceManager;
import org.jruby.truffle.extra.AttachmentsManager;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.stdlib.CoverageManager;

/**
 * Define a method from a module body (module/class/class << self ... end).
 */
@Instrumentable(factory = ModuleBodyDefinitionNodeWrapper.class)
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

    public ModuleBodyDefinitionNode(ModuleBodyDefinitionNode node) {
        this(node.getContext(), node.getSourceSection(), node.name, node.sharedMethodInfo, node.callTarget, node.captureBlock);
    }

    public InternalMethod executeMethod(VirtualFrame frame) {
        final DynamicObject dummyModule = coreLibrary().getObjectClass();
        final Visibility dummyVisibility = Visibility.PUBLIC;

        final DynamicObject capturedBlock;

        if (captureBlock) {
            capturedBlock = RubyArguments.getBlock(frame);
        } else {
            capturedBlock = null;
        }

        return new InternalMethod(sharedMethodInfo, name, dummyModule, dummyVisibility, false, null, callTarget, capturedBlock, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeMethod(frame);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == TraceManager.ClassTag.class) {
            return true;
        }

        return super.isTaggedWith(tag);
    }

}
