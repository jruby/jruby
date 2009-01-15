
package org.jruby.ext.ffi;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


@JRubyClass(name = "FFI::Buffer", parent = "FFI::" + AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS)
public final class Buffer extends AbstractMemory {
    /** Indicates that the Buffer is used for data copied IN to native memory */
    public static final int IN = 0x1;

    /** Indicates that the Buffer is used for data copied OUT from native memory */
    public static final int OUT = 0x2;
    
    /** The size of the primitive type this <tt>Buffer</tt> was allocated for */
    private final int typeSize;
    
    private final int inout;
    public static RubyClass createBufferClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Buffer",
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(Buffer.class);
        result.defineAnnotatedConstants(Buffer.class);

        return result;
    }

    public Buffer(Ruby runtime, RubyClass klass) {
        super(runtime, klass, new ArrayMemoryIO(0), 0);
        this.inout = IN | OUT;
        this.typeSize = 1;
    }
    Buffer(Ruby runtime, int size) {
        this(runtime, size, IN | OUT);
    }
    Buffer(Ruby runtime, int size, int flags) {
        this(runtime, FFIProvider.getModule(runtime).fastGetClass("Buffer"),
            new ArrayMemoryIO(size), size, 1, flags);
    }
    private Buffer(Ruby runtime, IRubyObject klass, MemoryIO io, long size, int typeSize, int inout) {
        super(runtime, (RubyClass) klass, io, size);
        this.typeSize = typeSize;
        this.inout = inout;
    }
    
    private static final int getCount(IRubyObject countArg) {
        return countArg instanceof RubyFixnum ? RubyFixnum.fix2int(countArg) : 1;
    }
    private static Buffer allocate(ThreadContext context, IRubyObject recv, 
            IRubyObject sizeArg, int count, int flags) {
        final int typeSize = calculateSize(context, sizeArg);
        final int total = typeSize * count;
        return new Buffer(context.getRuntime(), recv, new ArrayMemoryIO(total), total, typeSize, flags);
    }
    @JRubyMethod(name = { "new", "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        return allocate(context, recv, sizeArg, 1, IN | OUT);
    }
    @JRubyMethod(name = { "new", "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject arg2) {
        return allocate(context, recv, sizeArg, getCount(arg2), IN | OUT);
    }
    @JRubyMethod(name = { "new", "alloc_inout", "__alloc_inout" }, meta = true)
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
        return RubyString.newString(context.getRuntime(),
                String.format("#<Buffer size=%d>", size));
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
        return new Buffer(context.getRuntime(), getMetaClass(), io.slice(offset), size - offset, typeSize, this.inout);
    }
    ArrayMemoryIO getArrayMemoryIO() {
        return (ArrayMemoryIO) getMemoryIO();
    }
    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new Buffer(runtime, getMetaClass(), this.io.slice(offset), this.size - offset, this.typeSize, this.inout);
    }
    protected Pointer getPointer(Ruby runtime, long offset) {
        DirectMemoryIO ptr = (DirectMemoryIO) getMemoryIO().getMemoryIO(offset);
        return new BasePointer(runtime, ptr != null && !ptr.isNull() ? ptr : new NullMemoryIO(runtime));
    }
    public int getInOutFlags() {
        return inout;
    }
}
