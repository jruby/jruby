package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Struct;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 */
public final class DirectStructParameterStrategy extends PointerParameterStrategy {
    public DirectStructParameterStrategy() {
        super(true);
    }

    @Override
    public long getAddress(IRubyObject parameter) {
        return ((Pointer) ((Struct) parameter).getMemory()).getAddress();
    }

    @Override
    public Object array(IRubyObject parameter) {
        throw new RuntimeException("no array");
    }

    @Override
    public int arrayOffset(IRubyObject parameter) {
        throw new RuntimeException("no array");
    }

    @Override
    public int arrayLength(IRubyObject parameter) {
        throw new RuntimeException("no array");
    }
}
