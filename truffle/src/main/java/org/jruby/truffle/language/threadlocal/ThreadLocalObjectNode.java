/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.threadlocal;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

public abstract class ThreadLocalObjectNode extends RubyNode {

    @Override
    public abstract DynamicObject executeDynamicObject(VirtualFrame frame);

    @Specialization(
            guards = "cachedThread == getCurrentThread(frame)",
            limit = "getCacheLimit()"
    )
    protected DynamicObject getThreadLocalObjectCached(
            VirtualFrame frame,
            @Cached("getCurrentThread(frame)") DynamicObject cachedThread,
            @Cached("getThreadLocals(cachedThread)") DynamicObject cachedThreadLocals) {
        return cachedThreadLocals;
    }

    @Specialization(contains = "getThreadLocalObjectCached")
    protected DynamicObject getThreadLocalObjectUncached(VirtualFrame frame) {
        return getThreadLocals(getCurrentThread(frame));
    }

    protected DynamicObject getCurrentThread(VirtualFrame frame) {
        return getContext().getThreadManager().getCurrentThread();
    }

    protected DynamicObject getThreadLocals(DynamicObject thread) {
        return Layouts.THREAD.getThreadLocals(thread);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().THREAD_CACHE;
    }

}
