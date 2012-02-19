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
    public long getAddress(IRubyObject parameter) {
        RubyString s = (RubyString) parameter;
        Object existingHandle = s.getFFIHandle();
        if (existingHandle instanceof NativeStringHandle) {
            NativeStringHandle sh = (NativeStringHandle) existingHandle;
            if (s.getByteList() == sh.bl) {
                return sh.memory.getAddress();
            }
        }

        ByteList bl = s.getByteList();
        StringSupport.checkStringSafety(parameter.getRuntime(), parameter);
        DirectMemoryIO memory = TransientNativeMemoryIO.allocateAligned(parameter.getRuntime(), bl.length() + 1, 1, false);
        memory.putZeroTerminatedByteArray(0, bl.getUnsafeBytes(), bl.begin(), bl.length());
        s.setByteListShared();
        s.setFFIHandle(new NativeStringHandle(memory, s.getByteList()));

        return memory.getAddress();
    }

    @Override
    public Object array(IRubyObject parameter) {
        StringSupport.checkStringSafety(parameter.getRuntime(), parameter);
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
