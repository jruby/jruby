
package org.jruby.ext.ffi;


import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

import static org.jruby.runtime.Visibility.PRIVATE;

@JRubyClass(name = "FFI::MemoryPointer", parent = "FFI::Pointer")
public class MemoryPointer extends Pointer {
    
    public static RubyClass createMemoryPointerClass(Ruby runtime, RubyModule module) {
        RubyClass memptrClass = module.defineClassUnder("MemoryPointer",
                module.getClass("Pointer"),
                Options.REIFY_FFI.load() ? new ReifyingAllocator(MemoryPointer.class) : MemoryPointer::new);
        memptrClass.defineAnnotatedMethods(MemoryPointer.class);
        memptrClass.defineAnnotatedConstants(MemoryPointer.class);
        memptrClass.setReifiedClass(MemoryPointer.class);
        memptrClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof MemoryPointer && super.isKindOf(obj, type);
            }
        };

        return memptrClass;
    }

    public MemoryPointer(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    private MemoryPointer(Ruby runtime, IRubyObject klass, MemoryIO io, long total, int typeSize) {
        super(runtime, (RubyClass) klass, io, total, typeSize);
    }

    private final IRubyObject init(ThreadContext context, IRubyObject rbTypeSize, int count, int align, boolean clear, Block block) {
        typeSize = calculateTypeSize(context, rbTypeSize);
        size = typeSize * count;
        if (size < 0) {
            throw context.runtime.newArgumentError(String.format("Negative size (%d objects of %d size)", count, typeSize));
        }
        setMemoryIO(Factory.getInstance().allocateDirectMemory(context.runtime,
                size > 0 ? (int) size : 1, align, clear));
        if (getMemoryIO() == null) {
            Ruby runtime = context.runtime;
            throw RaiseException.from(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d objects of %d bytes", typeSize, count));
        }
        
        if (block.isGiven()) {
            try {
                return block.yield(context, this);
            } finally {
                ((AllocatedDirectMemoryIO) getMemoryIO()).free();
                setMemoryIO(new FreedMemoryIO(context.runtime));
            }
        } else {
            return this;
        }
    }

    static MemoryPointer allocate(Ruby runtime, int typeSize, int count, boolean clear) {
        final int total = typeSize * count;
        MemoryIO io = Factory.getInstance().allocateDirectMemory(runtime, total > 0 ? total : 1, clear);
        if (io == null) {
            throw RaiseException.from(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d objects of %d bytes", count, typeSize));
        }

        return new MemoryPointer(runtime, runtime.getFFI().memptrClass, io, total, typeSize);
    }

    @JRubyMethod(name = "from_string", meta = true)
    public static IRubyObject from_string(ThreadContext context, IRubyObject klass, IRubyObject s) {
        org.jruby.util.ByteList bl = s.convertToString().getByteList();
        MemoryPointer ptr = (MemoryPointer) ((RubyClass) klass).newInstance(context, context.runtime.newFixnum(bl.length() + 1), Block.NULL_BLOCK);
        ptr.getMemoryIO().putZeroTerminatedByteArray(0, bl.unsafeBytes(), bl.begin(), bl.length());

        return ptr;
    }

    @JRubyMethod(name = { "initialize" }, visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject sizeArg, Block block) {
        return sizeArg instanceof RubyFixnum
                ? init(context, RubyFixnum.one(context.runtime),
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

    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return RubyBoolean.newBoolean(context, this == obj
                || getAddress() == 0L && obj.isNil()
                || (obj instanceof MemoryPointer
                && ((MemoryPointer) obj).getAddress() == getAddress())
                && ((MemoryPointer) obj).getSize() == getSize());
    }
    
    @JRubyMethod(name = "free")
    public final IRubyObject free(ThreadContext context) {
        ((AllocatedDirectMemoryIO) getMemoryIO()).free();
        // Replace memory object with one that throws an exception on any access
        setMemoryIO(new FreedMemoryIO(context.runtime));
        return context.nil;
    }

    @JRubyMethod(name = "autorelease=", required = 1)
    public final IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        ((AllocatedDirectMemoryIO) getMemoryIO()).setAutoRelease(release.isTrue());
        return context.nil;
    }

    @JRubyMethod(name = "autorelease?")
    public final IRubyObject autorelease_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, ((AllocatedDirectMemoryIO) getMemoryIO()).isAutoRelease());
    }
}
