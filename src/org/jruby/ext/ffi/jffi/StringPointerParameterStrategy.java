package org.jruby.ext.ffi.jffi;

import org.jruby.RubyString;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 */
public class StringPointerParameterStrategy extends PointerParameterStrategy {
    public StringPointerParameterStrategy() {
        super(true);
    }

    @Override
    public long getAddress(IRubyObject parameter) {
        RubyString s = (RubyString) parameter;
        Object existingHandle = s.getFFIHandle();
        if (existingHandle instanceof StringHandle) {
            StringHandle sh = (StringHandle) existingHandle;
            if (s.getByteList() == sh.bl) {
                return sh.memory.getAddress();
            }
        }

        ByteList bl = s.getByteList();
        DirectMemoryIO memory = TransientNativeMemoryIO.allocateAligned(parameter.getRuntime(), bl.length() + 1, 1, false);
        memory.putZeroTerminatedByteArray(0, bl.getUnsafeBytes(), bl.begin(), bl.length());
        s.setByteListShared();
        s.setFFIHandle(new StringHandle(memory, s.getByteList()));

        return memory.getAddress();
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
    
    private static final class StringHandle {
        final ByteList bl;
        final DirectMemoryIO memory;
        
        StringHandle(DirectMemoryIO memory, ByteList bl) {
            this.memory = memory;
            this.bl = bl;
        }
    }
}
