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
}
