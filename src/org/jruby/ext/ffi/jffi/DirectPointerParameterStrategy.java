package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.DirectMemoryIO;
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
//        System.out.printf("direct pointer strategy returning address=%x\n", ((DirectMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).getAddress());
        return ((DirectMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).getAddress();
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
