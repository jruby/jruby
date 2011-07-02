package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public final class DelegatingPointerParameterStrategy extends PointerParameterStrategy {
    private final IRubyObject value;
    private final PointerParameterStrategy strategy;

    public DelegatingPointerParameterStrategy(IRubyObject value, PointerParameterStrategy strategy) {
        super(strategy.isDirect());
        this.value = value;
        this.strategy = strategy;
    }

    @Override
    public long getAddress(IRubyObject parameter) {
        return strategy.getAddress(value);
    }

    @Override
    public Object array(IRubyObject parameter) {
        return strategy.array(value);
    }

    @Override
    public int arrayOffset(IRubyObject parameter) {
        return strategy.arrayOffset(value);
    }

    @Override
    public int arrayLength(IRubyObject parameter) {
        return strategy.arrayLength(value);
    }
}
