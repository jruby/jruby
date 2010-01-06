/*
 * Copyright (C) 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public final class NativeObjectAllocator implements ObjectAllocator {
    private final long function;

    public NativeObjectAllocator(long function) {
        this.function = function;
    }

    public IRubyObject allocate(Ruby runtime, RubyClass klass) {
        ThreadContext context = runtime.getCurrentContext();
        ExecutionLock.lock(context);
        try {
            return Native.getInstance(runtime).callMethod(runtime.getCurrentContext(),
                    function, klass, 0, new IRubyObject[0]);
        } finally {
            ExecutionLock.unlock(context);
        }
    }
}
