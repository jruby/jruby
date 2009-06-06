
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * C memory pointer operations.
 * <p>
 * This is an abstract class that defines Pointer operations
 * </p>
 */
@JRubyClass(name="FFI::Pointer", parent=AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS)
public class Pointer extends AbstractMemory {
    public static RubyClass createPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Pointer",
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                PointerAllocator.INSTANCE);

        result.defineAnnotatedMethods(Pointer.class);
        result.defineAnnotatedConstants(Pointer.class);


        module.defineClassUnder("NullPointerError", runtime.getRuntimeError(),
                runtime.getRuntimeError().getAllocator());

        // Add Pointer::NULL as a constant
        result.fastSetConstant("NULL", new Pointer(runtime, result, new NullMemoryIO(runtime)));

        return result;
    }

    private static final class PointerAllocator implements ObjectAllocator {
        static final ObjectAllocator INSTANCE = new PointerAllocator();

        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new Pointer(runtime, klazz);
        }
    }

    private Pointer(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz, new NullMemoryIO(runtime), 0);
    }

    public Pointer(Ruby runtime, DirectMemoryIO io) {
        this(runtime, getPointerClass(runtime), io);
    }
    public Pointer(Ruby runtime, DirectMemoryIO io, long size, int typeSize) {
        this(runtime, getPointerClass(runtime), io, size, typeSize);
    }
    protected Pointer(Ruby runtime, RubyClass klass, DirectMemoryIO io) {
        super(runtime, klass, io, Long.MAX_VALUE);
    }
    protected Pointer(Ruby runtime, RubyClass klass, DirectMemoryIO io, long size) {
        super(runtime, klass, io, size);
    }
    protected Pointer(Ruby runtime, RubyClass klass, DirectMemoryIO io, long size, int typeSize) {
        super(runtime, klass, io, size, typeSize);
    }

    public static final RubyClass getPointerClass(Ruby runtime) {
        return runtime.fastGetModule("FFI").fastGetClass("Pointer");
    }

    @JRubyMethod(name = { "initialize" })
    public IRubyObject initialize(ThreadContext context, IRubyObject address) {
        io = Factory.getInstance().wrapDirectMemory(context.getRuntime(), RubyFixnum.num2long(address));
        size = Long.MAX_VALUE;
        typeSize = 1;

        return this;
    }

    @JRubyMethod(name = { "initialize" })
    public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject address) {
        io = Factory.getInstance().wrapDirectMemory(context.getRuntime(), RubyFixnum.num2long(address));
        size = Long.MAX_VALUE;
        typeSize = calculateSize(context, type);

        return this;
    }

    /**
     * Tests if this <tt>Pointer</tt> represents the C <tt>NULL</tt> value.
     *
     * @return true if the address is NULL.
     */
    @JRubyMethod(name = "null?")
    public IRubyObject null_p(ThreadContext context) {
        return context.getRuntime().newBoolean(getMemoryIO().isNull());
    }


    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        return RubyString.newString(context.getRuntime(),
                String.format("Pointer [address=%x]", getAddress()));
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        String hex = Long.toHexString(getAddress());
        return RubyString.newString(context.getRuntime(),
                String.format("#<Pointer address=0x%s>", hex));
    }

    @JRubyMethod(name = { "address", "to_i" })
    public IRubyObject address(ThreadContext context) {
        return context.getRuntime().newFixnum(getAddress());
    }

    /**
     * Gets the native memory address of this pointer.
     *
     * @return A long containing the native memory address.
     */
    public final long getAddress() {
        return ((DirectMemoryIO) getMemoryIO()).getAddress();
    }
    
    @Override
    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new Pointer(runtime, getPointerClass(runtime),
                (DirectMemoryIO) getMemoryIO().slice(offset),
                size == Long.MAX_VALUE ? Long.MAX_VALUE : size - offset, typeSize);
    }

    protected Pointer getPointer(Ruby runtime, long offset) {
        return new Pointer(runtime, getPointerClass(runtime), getMemoryIO().getMemoryIO(offset), Long.MAX_VALUE);
    }

}
