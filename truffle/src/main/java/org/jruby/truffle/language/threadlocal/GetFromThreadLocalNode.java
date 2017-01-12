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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

public class GetFromThreadLocalNode extends RubyNode {

    @Child private RubyNode value;

    public GetFromThreadLocalNode(RubyNode value) {
        this.value = value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return value.isDefined(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object threadLocalObject = value.execute(frame);

        if (RubyGuards.isThreadLocal(threadLocalObject)) {
            return getThreadLocalValue((ThreadLocalObject) threadLocalObject);
        }

        return threadLocalObject;
    }

    @TruffleBoundary
    private Object getThreadLocalValue(ThreadLocalObject threadLocalObject) {
        return threadLocalObject.get();
    }

}
