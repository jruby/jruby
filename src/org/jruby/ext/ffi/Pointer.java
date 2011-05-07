
package org.jruby.ext.ffi;

import java.nio.ByteOrder;
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
import static org.jruby.runtime.Visibility.*;

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

    public static final Pointer getNull(Ruby runtime) {
        return (Pointer) runtime.fastGetModule("FFI").fastGetClass("Pointer").fastGetConstant("NULL");
    }

    Pointer(Ruby runtime, RubyClass klazz) {
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

    public final AbstractMemory order(Ruby runtime, ByteOrder order) {
        return new Pointer(runtime,
                order.equals(getMemoryIO().order()) ? (DirectMemoryIO) getMemoryIO() : new SwappedMemoryIO(runtime, getMemoryIO()),
                size, typeSize);
    }

    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject address) {
        io = address instanceof Pointer
                ? ((Pointer) address).getMemoryIO()
                : Factory.getInstance().wrapDirectMemory(context.getRuntime(), RubyFixnum.num2long(address));
        size = Long.MAX_VALUE;
        typeSize = 1;

        return this;
    }

    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject address) {
        io = address instanceof Pointer
                ? ((Pointer) address).getMemoryIO()
                : Factory.getInstance().wrapDirectMemory(context.getRuntime(), RubyFixnum.num2long(address));
        size = Long.MAX_VALUE;
        typeSize = calculateTypeSize(context, type);

        return this;
    }
    
    /**
     * 
     */
    @JRubyMethod(required = 1, visibility=PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        if (this == other) {
            return this;
        }
        Pointer orig = (Pointer) other;
        this.typeSize = orig.typeSize;
        this.size = orig.size;

        setMemoryIO(orig.getMemoryIO().dup());

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
    @JRubyMethod(name = { "to_s", "inspect" }, optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        String s = size != Long.MAX_VALUE
                ? String.format("#<%s address=0x%x size=%s>", getMetaClass().getName(), getAddress(), size)
                : String.format("#<%s address=0x%x>", getMetaClass().getName(), getAddress());

        return RubyString.newString(context.getRuntime(), s);
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

    @Override
    protected AbstractMemory slice(Ruby runtime, long offset, long size) {
        return new Pointer(runtime, getPointerClass(runtime),
                (DirectMemoryIO) getMemoryIO().slice(offset, size), size, typeSize);
    }

    protected Pointer getPointer(Ruby runtime, long offset) {
        return new Pointer(runtime, getPointerClass(runtime), getMemoryIO().getMemoryIO(offset), Long.MAX_VALUE);
    }

}
