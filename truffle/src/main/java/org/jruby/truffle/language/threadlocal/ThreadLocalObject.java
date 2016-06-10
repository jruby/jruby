/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.threadlocal;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jruby.truffle.RubyContext;

public class ThreadLocalObject extends ThreadLocal<Object> {

    private final RubyContext context;

    @TruffleBoundary
    public static ThreadLocalObject wrap(RubyContext context, Object value) {
        final ThreadLocalObject threadLocal = new ThreadLocalObject(context);
        threadLocal.set(value);
        return threadLocal;
    }

    public ThreadLocalObject(RubyContext context) {
        this.context = context;
    }

    @Override
    protected Object initialValue() {
        return context.getCoreLibrary().getNilObject();
    }

}
