
package org.jruby.ext.ffi;

import java.nio.ByteOrder;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

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
        RubyClass pointerClass = module.defineClassUnder("Pointer",
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                Options.REIFY_FFI.load() ? new ReifyingAllocator(Pointer.class) : Pointer::new);

        pointerClass.defineAnnotatedMethods(Pointer.class);
        pointerClass.defineAnnotatedConstants(Pointer.class);
        pointerClass.setReifiedClass(Pointer.class);
        pointerClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof Pointer && super.isKindOf(obj, type); 
            }
        };

        module.defineClassUnder("NullPointerError", runtime.getRuntimeError(),
                runtime.getRuntimeError().getAllocator());

        // Add Pointer::NULL as a constant
        Pointer nullPointer = new Pointer(runtime, pointerClass, new NullMemoryIO(runtime));
        pointerClass.setConstant("NULL", nullPointer);
        
        runtime.getNilClass().addMethod("to_ptr", new NilToPointerMethod(runtime.getNilClass(), nullPointer, "to_ptr"));

        return pointerClass;
    }

    public static final Pointer getNull(Ruby runtime) {
        return runtime.getFFI().nullPointer;
    }

    public Pointer(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz, runtime.getFFI().getNullMemoryIO(), 0);
    }

    public Pointer(Ruby runtime, MemoryIO io) {
        this(runtime, getPointerClass(runtime), io);
    }
    public Pointer(Ruby runtime, MemoryIO io, long size, int typeSize) {
        this(runtime, getPointerClass(runtime), io, size, typeSize);
    }
    protected Pointer(Ruby runtime, RubyClass klass, MemoryIO io) {
        super(runtime, klass, io, Long.MAX_VALUE);
    }
    protected Pointer(Ruby runtime, RubyClass klass, MemoryIO io, long size) {
        super(runtime, klass, io, size);
    }
    protected Pointer(Ruby runtime, RubyClass klass, MemoryIO io, long size, int typeSize) {
        super(runtime, klass, io, size, typeSize);
    }

    public static final RubyClass getPointerClass(Ruby runtime) {
        return runtime.getFFI().pointerClass;
    }

    public final AbstractMemory order(Ruby runtime, ByteOrder order) {
        return new Pointer(runtime,
                order.equals(getMemoryIO().order()) ? getMemoryIO() : new SwappedMemoryIO(runtime, getMemoryIO()),
                size, typeSize);
    }
    
    @JRubyMethod(name = "size", meta = true, visibility = PUBLIC)
    public static IRubyObject size(ThreadContext context, IRubyObject recv) {
        return RubyFixnum.newFixnum(context.getRuntime(), Factory.getInstance().sizeOf(NativeType.POINTER));
    }

    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject address) {
        setMemoryIO(address instanceof Pointer
                ? ((Pointer) address).getMemoryIO()
                : Factory.getInstance().wrapDirectMemory(context.runtime, RubyFixnum.num2long(address)));
        size = Long.MAX_VALUE;
        typeSize = 1;

        return this;
    }

    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject address) {
        setMemoryIO(address instanceof Pointer
                ? ((Pointer) address).getMemoryIO()
                : Factory.getInstance().wrapDirectMemory(context.runtime, RubyFixnum.num2long(address)));
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
        return RubyBoolean.newBoolean(context, getMemoryIO().isNull());
    }


    @Override
    @JRubyMethod(name = { "to_s", "inspect" }, optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        String s = size != Long.MAX_VALUE
                ? String.format("#<%s address=0x%x size=%s>", getMetaClass().getName(), getAddress(), size)
                : String.format("#<%s address=0x%x>", getMetaClass().getName(), getAddress());

        return RubyString.newString(context.runtime, s);
    }

    @JRubyMethod(name = { "address", "to_i" })
    public IRubyObject address(ThreadContext context) {
        return context.runtime.newFixnum(getAddress());
    }

    /**
     * Gets the native memory address of this pointer.
     *
     * @return A long containing the native memory address.
     */
    public final long getAddress() {
        return getMemoryIO().address();
    }

    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return RubyBoolean.newBoolean(context, this == obj
                || getAddress() == 0L && obj.isNil()
                || (obj instanceof Pointer && ((Pointer) obj).getAddress() == getAddress()));
    }
    
    @Override
    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new Pointer(runtime, getPointerClass(runtime),
                getMemoryIO().slice(offset),
                size == Long.MAX_VALUE ? Long.MAX_VALUE : size - offset, typeSize);
    }

    @Override
    protected AbstractMemory slice(Ruby runtime, long offset, long size) {
        return new Pointer(runtime, getPointerClass(runtime),
                getMemoryIO().slice(offset, size), size, typeSize);
    }

    protected Pointer getPointer(Ruby runtime, long offset) {
        return new Pointer(runtime, getPointerClass(runtime), getMemoryIO().getMemoryIO(offset), Long.MAX_VALUE);
    }

    private static final class NilToPointerMethod extends DynamicMethod {
        private static final Arity ARITY = Arity.NO_ARGUMENTS;
        private final Pointer nullPointer;

        private NilToPointerMethod(RubyModule implementationClass, Pointer nullPointer, String name) {
            super(implementationClass, Visibility.PUBLIC, name);
            this.nullPointer = nullPointer;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            ARITY.checkArity(context.runtime, args);
            return nullPointer;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
            return nullPointer;
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }
    }
}
