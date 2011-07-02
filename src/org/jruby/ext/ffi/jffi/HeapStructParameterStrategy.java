package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.Struct;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public final class HeapStructParameterStrategy extends PointerParameterStrategy {
    public HeapStructParameterStrategy() {
        super(false);
    }

    @Override
    public long getAddress(IRubyObject parameter) {
        return 0;
    }

    @Override
    public Object array(IRubyObject parameter) {
        return ((ArrayMemoryIO) ((Struct) parameter).getMemory().getMemoryIO()).array();
    }

    @Override
    public int arrayOffset(IRubyObject parameter) {
        return ((ArrayMemoryIO) ((Struct) parameter).getMemory().getMemoryIO()).arrayOffset();
    }

    @Override
    public int arrayLength(IRubyObject parameter) {
        return ((ArrayMemoryIO) ((Struct) parameter).getMemory().getMemoryIO()).arrayLength();
    }
}
