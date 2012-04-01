package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.ObjectParameterType;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.Struct;

/**
 *
 */
public final class HeapStructParameterStrategy extends PointerParameterStrategy {
    public HeapStructParameterStrategy() {
        super(HEAP, ObjectParameterType.create(ObjectParameterType.ARRAY, ObjectParameterType.ComponentType.BYTE));
    }

    @Override
    public long address(Object parameter) {
        return 0;
    }

    @Override
    public Object object(Object parameter) {
        return ((ArrayMemoryIO) ((Struct) parameter).getMemory().getMemoryIO()).array();
    }

    @Override
    public int offset(Object parameter) {
        return ((ArrayMemoryIO) ((Struct) parameter).getMemory().getMemoryIO()).arrayOffset();
    }

    @Override
    public int length(Object parameter) {
        return ((ArrayMemoryIO) ((Struct) parameter).getMemory().getMemoryIO()).arrayLength();
    }
}
