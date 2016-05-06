/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;

@NodeChildren({
        @NodeChild("method"),
        @NodeChild("arguments"),
        @NodeChild("block")
})
public abstract class CallBoundMethodNode extends RubyNode {

    @Child CallInternalMethodNode callInternalMethodNode;

    public CallBoundMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        callInternalMethodNode = CallInternalMethodNodeGen.create(context, sourceSection, null, null);
    }

    public abstract Object executeCallBoundMethod(VirtualFrame frame, DynamicObject method, Object[] arguments, DynamicObject block);

    @Specialization
    protected Object call(VirtualFrame frame, DynamicObject method, Object[] arguments, DynamicObject block) {
        final InternalMethod internalMethod = Layouts.METHOD.getMethod(method);
        final Object[] frameArguments = packArguments(method, internalMethod, arguments, block);

        return callInternalMethodNode.executeCallMethod(frame, internalMethod, frameArguments);
    }

    private Object[] packArguments(DynamicObject method, InternalMethod internalMethod, Object[] arguments, DynamicObject block) {
        return RubyArguments.pack(
                null,
                null,
                internalMethod,
                DeclarationContext.METHOD,
                null,
                Layouts.METHOD.getReceiver(method),
                block,
                arguments);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().DISPATCH_CACHE;
    }
}
