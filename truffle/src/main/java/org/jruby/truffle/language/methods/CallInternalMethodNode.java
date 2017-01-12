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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild("method"),
        @NodeChild(value = "arguments", type = RubyNode[].class)
})
public abstract class CallInternalMethodNode extends RubyNode {

    public abstract Object executeCallMethod(VirtualFrame frame, InternalMethod method, Object[] frameArguments);

    @Specialization(
            guards = "method.getCallTarget() == cachedCallTarget",
            // TODO(eregon, 12 June 2015) we should maybe check an Assumption here to remove the cache entry when the lookup changes (redefined method, hierarchy changes)
            limit = "getCacheLimit()")
    protected Object callMethodCached(VirtualFrame frame, InternalMethod method, Object[] frameArguments,
            @Cached("method.getCallTarget()") CallTarget cachedCallTarget,
            @Cached("create(cachedCallTarget)") DirectCallNode callNode) {
        return callNode.call(frame, frameArguments);
    }

    @Specialization
    protected Object callMethodUncached(VirtualFrame frame, InternalMethod method, Object[] frameArguments,
            @Cached("create()") IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(frame, method.getCallTarget(), frameArguments);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().DISPATCH_CACHE;
    }

}
