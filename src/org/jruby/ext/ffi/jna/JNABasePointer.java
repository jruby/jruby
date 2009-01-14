
package org.jruby.ext.ffi.jna;

import org.jruby.ext.ffi.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
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
 * Base pointer class for all JNA pointers.
 */
@JRubyClass(name = "FFI::BasePointer", parent = "FFI::Pointer")
public class JNABasePointer extends org.jruby.ext.ffi.Pointer implements JNAMemory {
    public static final String JNA_POINTER_NAME = "BasePointer";

    public static RubyClass createJNAPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(JNA_POINTER_NAME,
                module.getClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(JNABasePointer.class);
        result.defineAnnotatedConstants(JNABasePointer.class);

        return result;
    }
    
    JNABasePointer(Ruby runtime, MemoryIO io, long size) {
        super(runtime, FFIProvider.getModule(runtime).fastGetClass(JNA_POINTER_NAME),
                io, size);
    }
    JNABasePointer(Ruby runtime, MemoryIO io) {
        this(runtime, FFIProvider.getModule(runtime).fastGetClass(JNA_POINTER_NAME),
                io);
    }
    JNABasePointer(Ruby runtime, Pointer value) {
        this(runtime, value != null ? JNAMemoryIO.wrap(value) : new NullMemoryIO(runtime));
    }
    JNABasePointer(Ruby runtime, RubyClass klass, MemoryIO io, long size) {
        super(runtime, klass, io, size);
    }
    JNABasePointer(Ruby runtime, IRubyObject klass, MemoryIO io) {
        super(runtime, (RubyClass) klass, io, Long.MAX_VALUE);
    }
    
    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        Pointer address = getAddress();
        String hex = address != null ? address.toString() : "native@0x0";
        return RubyString.newString(context.getRuntime(), "Pointer [address=" + hex + "]");
    }
    Pointer getAddress() {
        return getMemoryIO() instanceof NullMemoryIO
                ? Pointer.NULL : ((JNAMemoryIO) getMemoryIO()).getAddress();
    }
    public Object getNativeMemory() {
        return getMemoryIO() instanceof NullMemoryIO
                ? Pointer.NULL : ((JNAMemoryIO) getMemoryIO()).getMemory();
    }
    static final long ptr2long(Pointer ptr) {
        return new PointerByReference(ptr).getPointer().getInt(0);
    }

    @JRubyMethod(name = "address")
    public IRubyObject address(ThreadContext context) {
        return context.getRuntime().newFixnum(ptr2long(getAddress()));
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        String hex = Long.toHexString(ptr2long(getAddress()));
        return RubyString.newString(context.getRuntime(),
                String.format("#<Pointer address=0x%s>", hex));
    }

    @JRubyMethod(name = "put_pointer", required = 2)
    public IRubyObject put_pointer(ThreadContext context, IRubyObject offset, IRubyObject value) {
        Pointer ptr;
        if (value instanceof JNABasePointer) {
            ptr = ((JNABasePointer) value).getAddress();
        } else if (value.isNil()) {
            ptr = Pointer.NULL;
        } else {
            throw context.getRuntime().newArgumentError("Cannot convert argument to pointer");
        }
        ((JNAMemoryIO) getMemoryIO()).putPointer(getOffset(offset), ptr);
        return this;
    }

    @Override
    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new JNABasePointer(runtime,
                FFIProvider.getModule(runtime).fastGetClass(JNA_POINTER_NAME),
                getMemoryIO().slice(offset), size == Long.MAX_VALUE ? Long.MAX_VALUE : size - offset);
    }

    protected JNABasePointer getPointer(Ruby runtime, long offset) {
        return new JNABasePointer(runtime, getMemoryIO().getMemoryIO(offset));
    }
}