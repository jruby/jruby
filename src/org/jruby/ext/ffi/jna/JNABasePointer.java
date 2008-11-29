
package org.jruby.ext.ffi.jna;

import org.jruby.ext.ffi.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
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
@JRubyClass(name = FFIProvider.MODULE_NAME + "::" + JNAMemoryPointer.MEMORY_POINTER_NAME, parent = FFIProvider.MODULE_NAME + "::" + AbstractMemoryPointer.className)
public class JNABasePointer extends AbstractMemoryPointer implements JNAMemory {
    public static final String JNA_POINTER_NAME = "JNAPointer";

    public static RubyClass createJNAPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(JNA_POINTER_NAME,
                module.getClass(AbstractMemoryPointer.className),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(JNABasePointer.class);
        result.defineAnnotatedConstants(JNABasePointer.class);

        return result;
    }

    public JNABasePointer(Ruby runtime, RubyClass klass) {
        super(runtime, klass, JNAMemoryIO.wrap(Pointer.NULL), 0, 0);
    }
    JNABasePointer(Ruby runtime, RubyClass klass, MemoryIO io, long offset, long size) {
        super(runtime, klass, io, offset, size);
    }
    JNABasePointer(Ruby runtime, Pointer value) {
        this(runtime, JNAMemoryIO.wrap(value), 0, Long.MAX_VALUE);
    }
    JNABasePointer(Ruby runtime, JNABasePointer ptr, long offset) {
        this(runtime, ptr.io, ptr.offset + offset,
                ptr.size == Long.MAX_VALUE ? Long.MAX_VALUE : ptr.size - offset);
    }
    JNABasePointer(Ruby runtime, MemoryIO io, long offset, long size) {
        super(runtime, FFIProvider.getModule(runtime).fastGetClass(JNA_POINTER_NAME),
                io, offset, size);
    }

    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        Pointer address = getAddress();
        String hex = address != null ? address.toString() : "native@0x0";
        return RubyString.newString(context.getRuntime(), "Pointer [address=" + hex + "]");
    }
    Pointer getAddress() {
        return ((JNAMemoryIO) getMemoryIO()).getAddress();
    }
    public Object getNativeMemory() {
        return ((JNAMemoryIO) getMemoryIO()).slice(offset).getMemory();
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
        String hex = Long.toHexString(ptr2long(getAddress()) + offset);
        return RubyString.newString(context.getRuntime(),
                String.format("#<Pointer address=0x%s>", hex));
    }
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(ThreadContext context, IRubyObject value) {
        return new JNABasePointer(context.getRuntime(), this,
                RubyNumeric.fix2long(value));
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

    protected AbstractMemoryPointer getPointer(Ruby runtime, long offset) {
        return new JNABasePointer(runtime,
                getMemoryIO().getMemoryIO(this.offset + offset), 0, Long.MAX_VALUE);
    }
}