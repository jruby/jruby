
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


@JRubyClass(name = "FFI::Buffer", parent = "FFI::" + AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS)
public final class Buffer extends AbstractMemory {
    /** Indicates that the Buffer is used for data copied IN to native memory */
    public static final int IN = 0x1;

    /** Indicates that the Buffer is used for data copied OUT from native memory */
    public static final int OUT = 0x2;
    
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
        super(runtime, klass, new ArrayMemoryIO(runtime, 0), 0, 0);
        this.inout = IN | OUT;
    }
    Buffer(Ruby runtime, int size) {
        this(runtime, size, IN | OUT);
    }
    Buffer(Ruby runtime, int size, int flags) {
        this(runtime, runtime.fastGetModule("FFI").fastGetClass("Buffer"),
            new ArrayMemoryIO(runtime, size), size, 1, flags);
    }
    Buffer(Ruby runtime, byte[] data, int offset, int size) {
        this(runtime, runtime.fastGetModule("FFI").fastGetClass("Buffer"),
            new ArrayMemoryIO(runtime, data, offset, size), size, 1, IN | OUT);
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
        final int typeSize = calculateSize(context, sizeArg);
        final int total = typeSize * count;
        return new Buffer(context.getRuntime(), recv, 
                new ArrayMemoryIO(context.getRuntime(), total), total, typeSize, flags);
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
    
    ArrayMemoryIO getArrayMemoryIO() {
        return (ArrayMemoryIO) getMemoryIO();
    }
    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new Buffer(runtime, getMetaClass(), this.io.slice(offset), this.size - offset, this.typeSize, this.inout);
    }
    protected Pointer getPointer(Ruby runtime, long offset) {
        return new Pointer(runtime, (DirectMemoryIO) getMemoryIO().getMemoryIO(offset));
    }
    public int getInOutFlags() {
        return inout;
    }
}
