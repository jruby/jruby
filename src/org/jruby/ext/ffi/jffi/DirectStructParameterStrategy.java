package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.Struct;

/**
 * 
 */
public final class DirectStructParameterStrategy extends PointerParameterStrategy {
    public DirectStructParameterStrategy() {
        super(true);
    }

    @Override
    public long address(Object parameter) {
        return ((DirectMemoryIO) ((Struct) parameter).getMemory().getMemoryIO()).getAddress();
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
