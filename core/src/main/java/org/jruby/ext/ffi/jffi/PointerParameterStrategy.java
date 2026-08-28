package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.ObjectParameterStrategy;
import com.kenai.jffi.ObjectParameterType;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.runtime.builtin.IRubyObject;

abstract public class PointerParameterStrategy extends ObjectParameterStrategy {
    private final boolean isReferenceRequired;

    PointerParameterStrategy(boolean isDirect, boolean isReferenceRequired) {
        super(isDirect);
        this.isReferenceRequired = isReferenceRequired;
    }

    PointerParameterStrategy(boolean isDirect, boolean isReferenceRequired, ObjectParameterType objectType) {
        super(isDirect, objectType);
        this.isReferenceRequired = isReferenceRequired;
    }

    public final boolean isReferenceRequired() {
        return isReferenceRequired;
    }

    public abstract MemoryIO getMemoryIO(Object parameter);

    @Override
    public long address(Object parameter) {
        return getMemoryIO(parameter).address();
    }

    @Override
    public Object object(Object parameter) {
        return getMemoryIO(parameter).array();
    }

    @Override
    public int offset(Object parameter) {
        return getMemoryIO(parameter).arrayOffset();
    }

    @Override
    public int length(Object parameter) {
        return getMemoryIO(parameter).arrayLength();
    }
}
