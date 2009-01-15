
package org.jruby.ext.ffi;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::MemoryPointer", parent = "FFI::BasePointer")
public final class MemoryPointer extends BasePointer {
    private static final Factory factory = Factory.getInstance();
    private final int typeSize;

    public static RubyClass createMemoryPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("MemoryPointer",
                module.getClass(BasePointer.BASE_POINTER_NAME),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(MemoryPointer.class);
        result.defineAnnotatedConstants(MemoryPointer.class);

        return result;
    }

    private MemoryPointer(Ruby runtime, IRubyObject klass, DirectMemoryIO io, long total, int typeSize) {
        super(runtime, (RubyClass) klass, io, total);
        this.typeSize = typeSize;
    }
    
    private static final IRubyObject allocate(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, int count, boolean clear, Block block) {
        int typeSize = calculateSize(context, sizeArg);
        int total = typeSize * count;
        if (total < 0) {
            throw context.getRuntime().newArgumentError(String.format("Negative size (%d objects of %d size)", count, typeSize));
        }
        AllocatedDirectMemoryIO io = factory.allocateDirectMemory(total > 0 ? total : 1, clear);
        if (io == null) {
            Ruby runtime = context.getRuntime();
            throw new RaiseException(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d objects of %d bytes", typeSize, count), true);
        }
        MemoryPointer ptr = new MemoryPointer(context.getRuntime(), recv, io, total, typeSize);
        if (block.isGiven()) {
            try {
                return block.yield(context, ptr);
            } finally {
                io.free();
            }
        } else {
            return ptr;
        }
    }
    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, Block block) {
        return allocate(context, recv, sizeArg, 1, true, block);
    }
    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject count, Block block) {
        return allocate(context, recv, sizeArg, RubyNumeric.fix2int(count), true, block);
    }
    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject count, IRubyObject clear, Block block) {
        return allocate(context, recv, sizeArg, RubyNumeric.fix2int(count), clear.isTrue(), block);
    }

    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public final IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        return RubyString.newString(context.getRuntime(),
                String.format("MemoryPointer[address=%#x size=%d]", getAddress(), size));
    }

    @Override
    @JRubyMethod(name = "inspect")
    public final IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.getRuntime(),
                String.format("#<MemoryPointer address=%#x size=%d>", getAddress(), size));
    }

    /**
     * Indicates how many bytes the type that the pointer is cast as uses.
     * 
     * @param context
     * @return
     */
    @JRubyMethod(name = "type_size")
    public final IRubyObject type_size(ThreadContext context) {
        return context.getRuntime().newFixnum(typeSize);
    }
    
    @JRubyMethod(name = "[]")
    public final IRubyObject slice(ThreadContext context, IRubyObject indexArg) {
        final int index = RubyNumeric.num2int(indexArg);
        final int offset = index * typeSize;
        if (offset >= size) {
            throw context.getRuntime().newIndexError(String.format("Index %d out of range", index));
        }
        return new MemoryPointer(context.getRuntime(), getMetaClass(), (DirectMemoryIO) io.slice(offset),
                size - offset, typeSize);
    }

    @JRubyMethod(name = "free")
    public final IRubyObject free(ThreadContext context) {
        ((AllocatedDirectMemoryIO) getMemoryIO()).free();
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "autorelease=", required = 1)
    public final IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        ((AllocatedDirectMemoryIO) getMemoryIO()).setAutoRelease(release.isTrue());
        return context.getRuntime().getNil();
    }
}
