package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class PointerParameterStrategy {
    private final boolean isDirect;

    PointerParameterStrategy(boolean isDirect) {
        this.isDirect = isDirect;
    }

    public final boolean isDirect() {
        return isDirect;
    }

    abstract public long getAddress(IRubyObject parameter);
}
