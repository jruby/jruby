package org.jruby.ext.ffi.jffi;

import org.jruby.RubyInteger;
import org.jruby.ext.ffi.MemoryIO;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 */
final class IntegerPointerParameterStrategy extends PointerParameterStrategy {
    IntegerPointerParameterStrategy() {
        super(true, false);
    }

    public final MemoryIO getMemoryIO(Object parameter) {
        return getMemoryIO((RubyInteger) parameter);
    }

    static MemoryIO getMemoryIO(RubyInteger i) {
        return NativeMemoryIO.wrap(i.getRuntime(), i.getLongValue());
    }
}
