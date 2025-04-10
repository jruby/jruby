package org.jruby.ext.ffi.jffi;

import org.jruby.RubyString;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

public final class StringParameterStrategy extends PointerParameterStrategy {
    private final boolean checkStringSafety;

    public StringParameterStrategy(boolean isDirect, boolean checkStringSafety) {
        super(isDirect, isDirect);
        this.checkStringSafety = checkStringSafety;
    }

    @Override
    public long address(Object parameter) {
        return getMemoryIO(parameter).address();
    }

    public final MemoryIO getMemoryIO(Object parameter) {
        return getMemoryIO((RubyString) parameter, isDirect(), checkStringSafety);
    }
    
    static MemoryIO getMemoryIO(RubyString s, boolean isDirect, boolean checkStringSafety) {
        Object existingHandle = s.getFFIHandle();
        if (existingHandle instanceof NativeStringHandle) {
            NativeStringHandle sh = (NativeStringHandle) existingHandle;
            if (s.getByteList() == sh.bl && sh.memory.isDirect() == isDirect) {
                return sh.memory;
            }
        }

        ByteList bl = s.getByteList();
        if (checkStringSafety) StringSupport.checkStringSafety(s.getRuntime(), s);
        MemoryIO memory;
        if (isDirect) {
            memory = TransientNativeMemoryIO.allocateAligned(s.getRuntime(), bl.length() + 1, 1, false);
            memory.putZeroTerminatedByteArray(0, bl.unsafeBytes(), bl.begin(), bl.length());
        } else {
            memory = new ArrayMemoryIO(s.getRuntime(), bl.unsafeBytes(), bl.begin(), bl.length());
        }

        s.setByteListShared();
        s.setFFIHandle(new NativeStringHandle(memory, s.getByteList()));

        return memory;
    }

    @Override
    public Object object(Object parameter) {
        if (checkStringSafety) StringSupport.checkStringSafety(((IRubyObject) parameter).getRuntime(), (IRubyObject) parameter);
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
