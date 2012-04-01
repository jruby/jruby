package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.DirectMemoryIO;

/**
 *
 */
public final class DirectPointerParameterStrategy extends PointerParameterStrategy {
    DirectPointerParameterStrategy() {
        super(true);
    }

    @Override
    public final long address(Object parameter) {
//        System.out.printf("direct pointer strategy returning address=%x\n", ((DirectMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).getAddress());
        return ((DirectMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).getAddress();
    }

    @Override
    public Object object(Object parameter) {
        throw new RuntimeException("no array");
    }

    @Override
    public int offset(Object parameter) {
        throw new RuntimeException("no array");
    }

    @Override
    public int length(Object parameter) {
        throw new RuntimeException("no array");
    }
}
