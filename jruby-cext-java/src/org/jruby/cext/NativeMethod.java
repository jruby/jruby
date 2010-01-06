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

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public final class NativeMethod extends DynamicMethod {
    protected final Arity arity;
    protected final long function;

    public NativeMethod(RubyModule clazz, int arity, long function) {
        super(clazz, Visibility.PUBLIC, CallConfiguration.FrameBacktraceScopeFull);
        this.arity = Arity.createArity(arity);
        this.function = function;
    }

    @Override
    public final DynamicMethod dup() {
        return this;
    }

    @Override
    public final Arity getArity() {
        return arity;
    }

    @Override
    public final boolean isNative() {
        return true;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject recv, RubyModule clazz,
            String name, IRubyObject[] args, Block block) {

        ExecutionLock.lock(context);
        try {
            return Native.getInstance(context.getRuntime()).callMethod(context, function, recv, arity.getValue(), args);
        } finally {
            ExecutionLock.unlock(context);
        }
    }

}
