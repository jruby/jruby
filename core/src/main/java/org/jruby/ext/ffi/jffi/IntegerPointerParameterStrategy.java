package org.jruby.ext.ffi.jffi;

import org.jruby.RubyFixnum;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
final class IntegerPointerParameterStrategy extends PointerParameterStrategy {
    IntegerPointerParameterStrategy() {
        super(true, false);
    }

    public final MemoryIO getMemoryIO(Object parameter) {
        return getMemoryIO((IRubyObject) parameter);
    }

    static MemoryIO getMemoryIO(IRubyObject parameter) {
        return Factory.getInstance().wrapDirectMemory(parameter.getRuntime(), RubyFixnum.num2long(parameter));
    }
}
