package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.ObjectParameterType;
import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.ArrayMemoryIO;

/**
 *
 */
public final class HeapPointerParameterStrategy extends PointerParameterStrategy {
    public HeapPointerParameterStrategy() {
        super(HEAP, ObjectParameterType.create(ObjectParameterType.ARRAY, ObjectParameterType.ComponentType.BYTE));
    }

    @Override
    public long address(Object parameter) {
        return 0;
    }

    @Override
    public Object object(Object parameter) {
        return ((ArrayMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).array();
    }

    @Override
    public int offset(Object parameter) {
        return ((ArrayMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).arrayOffset();
    }

    @Override
    public int length(Object parameter) {
        return ((ArrayMemoryIO) ((AbstractMemory) parameter).getMemoryIO()).arrayLength();
    }
}
