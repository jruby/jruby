package org.jruby.ext.ffi.jffi;

import org.jruby.RubyString;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 *
 */
public class ConstStringPointerParameterStrategy extends PointerParameterStrategy {
    public ConstStringPointerParameterStrategy() {
        super(DIRECT);
    }

    @Override
    public long address(Object parameter) {
        RubyString s = (RubyString) parameter;
        Object existingHandle = s.getFFIHandle();
        if (existingHandle instanceof NativeStringHandle) {
            NativeStringHandle sh = (NativeStringHandle) existingHandle;
            if (s.getByteList() == sh.bl) {
                return sh.memory.getAddress();
            }
        }

        ByteList bl = s.getByteList();
        StringSupport.checkStringSafety(s.getRuntime(), s);
        DirectMemoryIO memory = TransientNativeMemoryIO.allocateAligned(s.getRuntime(), bl.length() + 1, 1, false);
        memory.putZeroTerminatedByteArray(0, bl.getUnsafeBytes(), bl.begin(), bl.length());
        s.setByteListShared();
        s.setFFIHandle(new NativeStringHandle(memory, s.getByteList()));

        return memory.getAddress();
    }

    @Override
    public Object object(Object parameter) {
        StringSupport.checkStringSafety(((IRubyObject) parameter).getRuntime(), (IRubyObject) parameter);
        return ((RubyString) parameter).getByteList().unsafeBytes();
    }

    @Override
    public int offset(Object parameter) {
        return ((RubyString) parameter).getByteList().begin();
    }

    @Override
    public int length(Object parameter) {
        return ((RubyString) parameter).getByteList().length();
    }

}
