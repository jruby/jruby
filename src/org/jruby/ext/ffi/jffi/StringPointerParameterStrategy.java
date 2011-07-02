package org.jruby.ext.ffi.jffi;

import org.jruby.RubyString;
import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class StringPointerParameterStrategy extends PointerParameterStrategy {
    public StringPointerParameterStrategy() {
        super(false);
    }

    @Override
    public long getAddress(IRubyObject parameter) {
        return 0;
    }

    @Override
    public Object array(IRubyObject parameter) {
        return ((RubyString) parameter).getByteList().unsafeBytes();
    }

    @Override
    public int arrayOffset(IRubyObject parameter) {
        return ((RubyString) parameter).getByteList().begin();
    }

    @Override
    public int arrayLength(IRubyObject parameter) {
        return ((RubyString) parameter).getByteList().length();
    }
}
