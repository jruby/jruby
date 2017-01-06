/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.cast.ProcOrNullNode;
import org.jruby.truffle.core.cast.ProcOrNullNodeGen;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;

@NodeChildren({
        @NodeChild("method"),
        @NodeChild("arguments"),
        @NodeChild("block")
})
public abstract class CallBoundMethodNode extends RubyNode {

    @Child private CallInternalMethodNode callInternalMethodNode = CallInternalMethodNodeGen.create(null, null);
    @Child private ProcOrNullNode procOrNullNode = ProcOrNullNodeGen.create(null);

    public abstract Object executeCallBoundMethod(VirtualFrame frame, DynamicObject method, Object[] arguments, Object block);

    @Specialization
    protected Object call(VirtualFrame frame, DynamicObject method, Object[] arguments, Object block) {
        final InternalMethod internalMethod = Layouts.METHOD.getMethod(method);
        final DynamicObject typedBlock = procOrNullNode.executeProcOrNull(block);
        final Object[] frameArguments = packArguments(method, internalMethod, arguments, typedBlock);

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
