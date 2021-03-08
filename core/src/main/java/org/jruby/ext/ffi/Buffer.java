
package org.jruby.ext.ffi;


import java.nio.ByteOrder;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.Visibility.*;


@JRubyClass(name = "FFI::Buffer", parent = "FFI::" + AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS)
public final class Buffer extends AbstractMemory {
    /** Indicates that the Buffer is used for data copied IN to native memory */
    public static final int IN = 0x1;

    /** Indicates that the Buffer is used for data copied OUT from native memory */
    public static final int OUT = 0x2;
    
    private int inout;

    public static RubyClass createBufferClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Buffer",
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                Buffer::new);
        result.defineAnnotatedMethods(Buffer.class);
        result.defineAnnotatedConstants(Buffer.class);

        return result;
    }

    public Buffer(Ruby runtime, RubyClass klass) {
        super(runtime, klass, runtime.getFFI().getNullMemoryIO(), 0, 0);
        this.inout = IN | OUT;
    }
    
    public Buffer(Ruby runtime, int size) {
        this(runtime, size, IN | OUT);
    }
    
    public Buffer(Ruby runtime, int size, int flags) {
        this(runtime, runtime.getFFI().bufferClass,
            allocateMemoryIO(runtime, size), size, 1, flags);
    }

    private Buffer(Ruby runtime, IRubyObject klass, MemoryIO io, long size, int typeSize, int inout) {
        super(runtime, (RubyClass) klass, io, size, typeSize);
        this.inout = inout;
    }
    
    private static final int getCount(IRubyObject countArg) {
        return countArg instanceof RubyFixnum ? RubyFixnum.fix2int(countArg) : 1;
    }
    
    private static Buffer allocate(ThreadContext context, IRubyObject recv, 
            IRubyObject sizeArg, int count, int flags) {
        final int typeSize = calculateTypeSize(context, sizeArg);
        final int total = typeSize * count;
        return new Buffer(context.runtime, recv,
                allocateMemoryIO(context.runtime, total), total, typeSize, flags);
    }

    private IRubyObject init(ThreadContext context, IRubyObject rbTypeSize, int count, int flags, Block block) {
        this.typeSize = calculateTypeSize(context, rbTypeSize);
        this.size = this.typeSize * count;
        this.inout = flags;
        setMemoryIO(allocateMemoryIO(context.runtime, (int) this.size));

        if (block.isGiven()) {
            block.yield(context, this);
        }

        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject sizeArg, Block block) {
        return sizeArg instanceof RubyFixnum
                ? init(context, RubyFixnum.one(context.runtime),
                    RubyFixnum.fix2int(sizeArg), (IN | OUT), block)
                : init(context, sizeArg, 1, (IN | OUT), block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject sizeArg, IRubyObject arg2, Block block) {
        return init(context, sizeArg, getCount(arg2), (IN | OUT), block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject sizeArg,
            IRubyObject countArg, IRubyObject clearArg, Block block) {
        return init(context, sizeArg, RubyFixnum.fix2int(countArg), (IN | OUT), block);
    }
    
    /**
     * 
     */
    @JRubyMethod(required = 1, visibility=PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        if (this == other) {
            return this;
        }
        Buffer orig = (Buffer) other;
        this.typeSize = orig.typeSize;
        this.size = orig.size;
        this.inout = orig.inout;
        
        setMemoryIO(orig.getMemoryIO().dup());
        return this;
    }

    @JRubyMethod(name = { "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        return allocate(context, recv, sizeArg, 1, IN | OUT);
    }

    @JRubyMethod(name = { "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject arg2) {
        return allocate(context, recv, sizeArg, getCount(arg2), IN | OUT);
    }

    @JRubyMethod(name = { "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv, 
            IRubyObject sizeArg, IRubyObject countArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, RubyFixnum.fix2int(countArg), IN | OUT);
    }

    @JRubyMethod(name = { "new_in", "alloc_in", "__alloc_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv, IRubyObject arg) {       
        return allocate(context, recv, arg, 1, IN);
    }

    @JRubyMethod(name = { "new_in", "alloc_in", "__alloc_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject arg2) {
        return allocate(context, recv, sizeArg, getCount(arg2), IN);
    }

    @JRubyMethod(name = { "new_in", "alloc_in", "__alloc_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject countArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, RubyFixnum.fix2int(countArg), IN);
    }

    @JRubyMethod(name = {  "new_out", "alloc_out", "__alloc_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        return allocate(context, recv, sizeArg, 1, OUT);
    }

    @JRubyMethod(name = {  "new_out", "alloc_out", "__alloc_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject arg2) {
        return allocate(context, recv, sizeArg, getCount(arg2), OUT);
    }

    @JRubyMethod(name = {  "new_out", "alloc_out", "__alloc_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject countArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, RubyFixnum.fix2int(countArg), OUT);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.runtime,
                String.format("#<Buffer size=%d>", size));
    }

    
    public final AbstractMemory order(Ruby runtime, ByteOrder order) {
        return new Buffer(runtime, getMetaClass(),
                order.equals(getMemoryIO().order()) ? getMemoryIO() : new SwappedMemoryIO(runtime, getMemoryIO()),
                size, typeSize, inout);
    }

    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new Buffer(runtime, getMetaClass(), getMemoryIO().slice(offset), this.size - offset, this.typeSize, this.inout);
    }

    protected AbstractMemory slice(Ruby runtime, long offset, long size) {
        return new Buffer(runtime, getMetaClass(), getMemoryIO().slice(offset, size), size, this.typeSize, this.inout);
    }

    protected Pointer getPointer(Ruby runtime, long offset) {
        return new Pointer(runtime, getMemoryIO().getMemoryIO(offset));
    }
    public int getInOutFlags() {
        return inout;
    }
    
    private static MemoryIO allocateMemoryIO(Ruby runtime, int size) {
        return Factory.getInstance().allocateTransientDirectMemory(runtime, size, 8, true);
    }
}
