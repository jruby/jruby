
package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.*;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Base pointer class for all JNA pointers.
 */
@JRubyClass(name = "FFI::BasePointer", parent = FFIProvider.MODULE_NAME + "::" + AbstractMemoryPointer.className)
public class BasePointer extends Pointer {

    public static final String BASE_POINTER_NAME = "BasePointer";

    public static RubyClass createJNAPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(BASE_POINTER_NAME,
                module.getClass(AbstractMemoryPointer.className),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(BasePointer.class);
        result.defineAnnotatedConstants(BasePointer.class);

        return result;
    }
    
    BasePointer(Ruby runtime, MemoryIO io, long offset, long size) {
        this(runtime, FFIProvider.getModule(runtime).fastGetClass(BASE_POINTER_NAME),
                io, offset, size);
    }
    BasePointer(Ruby runtime, long address) {
        this(runtime, 
                address != 0 ? new DirectMemoryIO(address) : new NullMemoryIO(runtime),
                0, Long.MAX_VALUE);
    }
    BasePointer(Ruby runtime, RubyClass klass, MemoryIO io, long offset, long size) {
        super(runtime, klass, io, offset, size);
    }

    BasePointer(Ruby runtime, IRubyObject klass, BasePointer ptr, long offset) {
        this(runtime, klass, ptr.io, ptr.offset + offset,
                ptr.size == Long.MAX_VALUE ? Long.MAX_VALUE : ptr.size - offset);
    }
    BasePointer(Ruby runtime, IRubyObject klass, MemoryIO io, long offset, long size) {
        super(runtime, (RubyClass) klass, io, offset, size);
    }

    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        return RubyString.newString(context.getRuntime(), 
                String.format("Pointer [address=%x]", getAddress()));
    }
    long getAddress() {
        return ((PointerMemoryIO) getMemoryIO()).getAddress() + getOffset();
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
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(ThreadContext context, IRubyObject value) {
        return new BasePointer(context.getRuntime(),
                FFIProvider.getModule(context.getRuntime()).fastGetClass(BASE_POINTER_NAME),
                this, RubyNumeric.fix2long(value));
    }

    @JRubyMethod(name = "put_pointer", required = 2)
    public IRubyObject put_pointer(ThreadContext context, IRubyObject offset, IRubyObject value) {
        long ptr;
        if (value instanceof BasePointer) {
            ptr = ((BasePointer) value).getAddress();
        } else if (value.isNil()) {
            ptr = 0;
        } else {
            throw context.getRuntime().newArgumentError("Cannot convert argument to pointer");
        }

        getMemoryIO().putAddress(getOffset(offset), ptr);
        return this;
    }

    protected BasePointer getPointer(Ruby runtime, long offset) {
        return new BasePointer(runtime,
                getMemoryIO().getMemoryIO(this.offset + offset), 0, Long.MAX_VALUE);
    }
}