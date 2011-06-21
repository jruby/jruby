package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public final class HeapPointerParameterStrategy extends PointerParameterStrategy {
    public HeapPointerParameterStrategy() {
        super(false);
    }

    @Override
    public long getAddress(IRubyObject parameter) {
        return 0;
    }
}
