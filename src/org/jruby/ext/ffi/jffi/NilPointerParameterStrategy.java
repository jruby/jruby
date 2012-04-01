package org.jruby.ext.ffi.jffi;

/**
 * 
 */
public final class NilPointerParameterStrategy extends PointerParameterStrategy {
    public NilPointerParameterStrategy() {
        super(true);
    }

    @Override
    public long address(Object parameter) {
        return 0L;
    }

    @Override
    public Object object(Object parameter) {
        return null;
    }

    @Override
    public int offset(Object parameter) {
        return 0;
    }

    @Override
    public int length(Object parameter) {
        return 0;
    }
}
