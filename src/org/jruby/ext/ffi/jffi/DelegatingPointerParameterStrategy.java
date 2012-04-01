package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.ObjectParameterType;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public final class DelegatingPointerParameterStrategy extends PointerParameterStrategy {
    private static final ObjectParameterType OBJECT_TYPE = ObjectParameterType.create(ObjectParameterType.ARRAY, ObjectParameterType.ComponentType.BYTE);
    private final IRubyObject value;
    private final PointerParameterStrategy strategy;

    public DelegatingPointerParameterStrategy(IRubyObject value, PointerParameterStrategy strategy) {
        super(strategy.isDirect(), OBJECT_TYPE);
        this.value = value;
        this.strategy = strategy;
    }

    @Override
    public long address(Object parameter) {
        return strategy.address(value);
    }

    @Override
    public Object object(Object parameter) {
        return strategy.object(value);
    }

    @Override
    public int offset(Object parameter) {
        return strategy.offset(value);
    }

    @Override
    public int length(Object parameter) {
        return strategy.length(value);
    }
}
