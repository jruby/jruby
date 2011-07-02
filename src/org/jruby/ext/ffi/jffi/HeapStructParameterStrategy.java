package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.ArrayMemoryIO;
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
        return ((ArrayMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).array();
    }

    @Override
    public int arrayOffset(IRubyObject parameter) {
        return ((ArrayMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).arrayLength();
    }

    @Override
    public int arrayLength(IRubyObject parameter) {
        return ((ArrayMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).arrayOffset();
    }
}
