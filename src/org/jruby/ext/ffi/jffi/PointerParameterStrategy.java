package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class PointerParameterStrategy {
    private final boolean isDirect;
    static enum StrategyType { DIRECT, HEAP }
    protected static final StrategyType DIRECT = StrategyType.DIRECT;
    protected static final StrategyType HEAP = StrategyType.HEAP;

    PointerParameterStrategy(boolean isDirect) {
        this.isDirect = isDirect;
    }

    PointerParameterStrategy(StrategyType type) {
        this.isDirect = type == StrategyType.DIRECT;
    }


    public final boolean isDirect() {
        return isDirect;
    }

    abstract public long getAddress(IRubyObject parameter);

    abstract public Object array(IRubyObject parameter);
    abstract public int arrayOffset(IRubyObject parameter);
    abstract public int arrayLength(IRubyObject parameter);
}
