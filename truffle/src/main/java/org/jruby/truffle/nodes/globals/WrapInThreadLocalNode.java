/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.globals;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.ThreadLocalObject;

/**
 * Wrap a child value in a new {@link ThreadLocal} so that a value can be stored in a location such as a frame without
 * making that value visible to other threads.
 *
 * This is used in combination with nodes that read and writes from storage locations such as frames to make them
 * thread-local.
 *
 * Also see {@link GetFromThreadLocalNode}.
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class WrapInThreadLocalNode extends RubyNode {

    public WrapInThreadLocalNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization
    public ThreadLocalObject wrap(Object value) {
        return ThreadLocalObject.wrap(getContext(), value);
    }

}
