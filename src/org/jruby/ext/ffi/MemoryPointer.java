
package org.jruby.ext.ffi;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.Visibility.*;

@JRubyClass(name = "FFI::MemoryPointer", parent = "FFI::Pointer")
public final class MemoryPointer extends Pointer {
    
    public static RubyClass createMemoryPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("MemoryPointer",
                module.fastGetClass("Pointer"),
                MemoryPointerAllocator.INSTANCE);
        result.defineAnnotatedMethods(MemoryPointer.class);
        result.defineAnnotatedConstants(MemoryPointer.class);

        return result;
    }

    private static final class MemoryPointerAllocator implements ObjectAllocator {
        static final ObjectAllocator INSTANCE = new MemoryPointerAllocator();

        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new MemoryPointer(runtime, klazz);
        }
    }


    private MemoryPointer(Ruby runtime, IRubyObject klass) {
        super(runtime, (RubyClass) klass);
    }

    private MemoryPointer(Ruby runtime, IRubyObject klass, DirectMemoryIO io, long total, int typeSize) {
        super(runtime, (RubyClass) klass, io, total, typeSize);
    }

    private final IRubyObject init(ThreadContext context, IRubyObject rbTypeSize, int count, int align, boolean clear, Block block) {
        typeSize = calculateTypeSize(context, rbTypeSize);
        size = typeSize * count;
        if (size < 0) {
            throw context.getRuntime().newArgumentError(String.format("Negative size (%d objects of %d size)", count, typeSize));
        }
        setMemoryIO(Factory.getInstance().allocateDirectMemory(context.getRuntime(),
                size > 0 ? (int) size : 1, align, clear));
        if (getMemoryIO() == null) {
            Ruby runtime = context.getRuntime();
            throw new RaiseException(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d objects of %d bytes", typeSize, count), true);
        }
        
        if (block.isGiven()) {
            try {
                return block.yield(context, this);
            } finally {
                ((AllocatedDirectMemoryIO) getMemoryIO()).free();
                setMemoryIO(new FreedMemoryIO(context.getRuntime()));
            }
        } else {
            return this;
        }
    }

    static final MemoryPointer allocate(Ruby runtime, int typeSize, int count, boolean clear) {
        final int total = typeSize * count;
        AllocatedDirectMemoryIO io = Factory.getInstance().allocateDirectMemory(runtime, total > 0 ? total : 1, clear);
        if (io == null) {
            throw new RaiseException(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d objects of %d bytes", typeSize, count), true);
        }

        return new MemoryPointer(runtime, runtime.fastGetModule("FFI").fastGetClass("MemoryPointer"), io, total, typeSize);
    }

    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject sizeArg, Block block) {
        return sizeArg instanceof RubyFixnum
                ? init(context, RubyFixnum.one(context.getRuntime()), 
                    RubyFixnum.fix2int(sizeArg), 1, true, block)
                : init(context, sizeArg, 1, 1, true, block);
    }
    
    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject sizeArg, IRubyObject count, Block block) {
        return init(context, sizeArg, RubyNumeric.fix2int(count), 1, true, block);
    }
    
    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context,
            IRubyObject sizeArg, IRubyObject count, IRubyObject clear, Block block) {
        return init(context, sizeArg, RubyNumeric.fix2int(count), 1, clear.isTrue(), block);
    }
    
    @Override
    public final String toString() {
        return String.format("MemoryPointer[address=%#x, size=%d]", getAddress(), size);
    }
    
    @JRubyMethod(name = "free")
    public final IRubyObject free(ThreadContext context) {
        ((AllocatedDirectMemoryIO) getMemoryIO()).free();
        // Replace memory object with one that throws an exception on any access
        setMemoryIO(new FreedMemoryIO(context.getRuntime()));
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "autorelease=", required = 1)
    public final IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        ((AllocatedDirectMemoryIO) getMemoryIO()).setAutoRelease(release.isTrue());
        return context.getRuntime().getNil();
    }
}
