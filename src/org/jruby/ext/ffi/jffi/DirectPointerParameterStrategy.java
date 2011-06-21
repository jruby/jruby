package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.Pointer;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public final class DirectPointerParameterStrategy extends PointerParameterStrategy {
    DirectPointerParameterStrategy() {
        super(true);
    }

    @Override
    public final long getAddress(IRubyObject parameter) {
        return ((Pointer) parameter).getAddress();
    }
}
