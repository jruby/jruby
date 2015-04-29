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

/**
 * If a child node produces a {@link ThreadLocal}, get the value from it. If the value is not a {@code ThreadLocal},
 * return it unmodified.
 *
 * This is used in combination with nodes that read and writes from storage locations such as frames to make them
 * thread-local.
 *
 * Also see {@link WrapInThreadLocalNode}.
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class GetFromThreadLocalNode extends RubyNode {

    public GetFromThreadLocalNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization
    public Object get(ThreadLocal<?> threadLocal) {
        return threadLocal.get();
    }

    @Specialization(guards = "!isThreadLocal(value)")
    public Object get(Object value) {
        return value;
    }

    public static Object get(RubyContext context, Object value) {
        if (value instanceof ThreadLocal) {
            return ((ThreadLocal<?>) value).get();
        } else {
            return value;
        }
    }


}
