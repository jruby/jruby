package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 */
public final class NilPointerParameterStrategy extends PointerParameterStrategy {
    public NilPointerParameterStrategy() {
        super(true);
    }

    @Override
    public long getAddress(IRubyObject parameter) {
        return 0L;
    }

    @Override
    public Object array(IRubyObject parameter) {
        return null;
    }

    @Override
    public int arrayOffset(IRubyObject parameter) {
        return 0;
    }

    @Override
    public int arrayLength(IRubyObject parameter) {
        return 0;
    }
}
