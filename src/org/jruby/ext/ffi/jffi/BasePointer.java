
package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.*;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Base pointer class for all JFFI pointers.
 */
@JRubyClass(name = "FFI::BasePointer", parent = "FFI::Pointer")
public class BasePointer extends Pointer {

    public static final String BASE_POINTER_NAME = "BasePointer";

    public static RubyClass createJNAPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(BASE_POINTER_NAME,
                module.getClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(BasePointer.class);
        result.defineAnnotatedConstants(BasePointer.class);

        return result;
    }
    private static final RubyClass getRubyClass(Ruby runtime) {
        return FFIProvider.getModule(runtime).fastGetClass(BASE_POINTER_NAME);
    }
    BasePointer(Ruby runtime, MemoryIO io, long size) {
        super(runtime, getRubyClass(runtime), io, size);
    }
    BasePointer(Ruby runtime, long address) {
        super(runtime, getRubyClass(runtime),
                address != 0 ? new NativeMemoryIO(address) : new NullMemoryIO(runtime));
    }
    BasePointer(Ruby runtime, RubyClass klass, MemoryIO io, long size) {
        super(runtime, klass, io, size);
    }
    
    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        return RubyString.newString(context.getRuntime(), 
                String.format("Pointer [address=%x]", getAddress()));
    }
    long getAddress() {
        return ((PointerMemoryIO) getMemoryIO()).getAddress();
    }
    
    @JRubyMethod(name = "address")
    public IRubyObject address(ThreadContext context) {
        return context.getRuntime().newFixnum(getAddress());
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        String hex = Long.toHexString(getAddress());
        return RubyString.newString(context.getRuntime(),
                String.format("#<Pointer address=0x%s>", hex));
    }
    
    @Override
    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new BasePointer(runtime, getRubyClass(runtime),
                getMemoryIO().slice(offset), size == Long.MAX_VALUE ? Long.MAX_VALUE : size - offset);
    }

    protected BasePointer getPointer(Ruby runtime, long offset) {
        MemoryIO ptr = getMemoryIO().getMemoryIO(offset);
        return new BasePointer(runtime,
                ptr != null && !ptr.isNull() ? ptr : new NullMemoryIO(runtime),
                Long.MAX_VALUE);
    }
}