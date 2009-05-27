
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
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
public abstract class Pointer extends AbstractMemory {
    public static RubyClass createPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Pointer",
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        result.defineAnnotatedMethods(Pointer.class);
        result.defineAnnotatedConstants(Pointer.class);

        return result;
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

    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject address) {
        return new BasePointer(context.getRuntime(),
                Factory.getInstance().wrapDirectMemory(context.getRuntime(), RubyFixnum.num2long(address)));
    }

    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv,
            IRubyObject type, IRubyObject address) {
        return new BasePointer(context.getRuntime(), 
                Factory.getInstance().wrapDirectMemory(context.getRuntime(), RubyFixnum.num2long(address)),
                Long.MAX_VALUE, calculateSize(context, type));
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
}
