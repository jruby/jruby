package org.jruby.ext.cffi;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class Pointer extends RubyObject implements NativeAddress {
    jnr.ffi.Pointer memory;
    Pointer(Ruby runtime, RubyClass metaClass, jnr.ffi.Pointer memory) {
        super(runtime, metaClass);
        this.memory = memory;
    }

    @Override
    public long address() {
        return memory.address();
    }
    
    static Pointer newPointer(Ruby runtime, long address) {
        return newPointer(runtime, jnr.ffi.Pointer.wrap(jnr.ffi.Runtime.getSystemRuntime(), address));
    }

    static Pointer newPointer(Ruby runtime, jnr.ffi.Pointer memory) {
        return new Pointer(runtime, JRubyCFFILibrary.getModule(runtime).getClass("Pointer"), memory);
    }

    @JRubyMethod(name = "malloc", module = true)
    public static IRubyObject malloc(ThreadContext context, IRubyObject self, IRubyObject size) {
        return newPointer(context.runtime, 
                jnr.ffi.Memory.allocateDirect(jnr.ffi.Runtime.getSystemRuntime(), (int) size.convertToInteger().getLongValue()));
    }
    
    @JRubyMethod(name = "from_string", module = true)
    public static IRubyObject from_string(ThreadContext context, IRubyObject self, IRubyObject obj) {
        ByteList bl = obj.convertToString().getByteList();
        jnr.ffi.Pointer memory = jnr.ffi.Memory.allocateDirect(jnr.ffi.Runtime.getSystemRuntime(), bl.length());
        memory.put(0, bl.unsafeBytes(), bl.begin(), bl.length());
        return newPointer(context.runtime, memory);
    }
    
    @JRubyMethod(name = "read_string")
    public IRubyObject read_string(ThreadContext context) {
        int len = memory.indexOf(0, (byte) 0);
        byte[] string = new byte[len > 0 ? len : 0]; 
        memory.get(0, string, 0, len);
        return RubyString.newStringNoCopy(context.runtime, string);
    }
    
    @JRubyMethod(name = "null?")
    public IRubyObject null_p(ThreadContext context) {
        return context.runtime.newBoolean(memory.address() == 0);
    }
}
