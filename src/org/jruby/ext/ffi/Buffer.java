
package org.jruby.ext.ffi;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;


@JRubyClass(name = "FFI::Buffer", parent = FFIProvider.MODULE_NAME + "::" + AbstractMemoryPointer.className)
public final class Buffer extends AbstractMemory {
    private static final Factory factory = Factory.getInstance();

    private static final boolean CLEAR_DEFAULT = true;

    public static RubyClass createBufferClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Buffer",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(AbstractMemory.class);
        result.defineAnnotatedConstants(AbstractMemory.class);
        result.defineAnnotatedMethods(Buffer.class);
        result.defineAnnotatedConstants(Buffer.class);

        return result;
    }

    public Buffer(Ruby runtime, RubyClass klass) {
        super(runtime, klass, factory.allocateHeapMemory(0, CLEAR_DEFAULT), 0, 0);
    }

    private Buffer(Ruby runtime, IRubyObject klass, Buffer ptr, long offset) {
        super(runtime, (RubyClass) klass, ptr.io, ptr.offset + offset, ptr.size - offset);
    }
    private Buffer(Ruby runtime, IRubyObject klass, MemoryIO io, long offset, long size) {
        super(runtime, (RubyClass) klass, io, offset, size);
    }
    
    private static Buffer allocate(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, boolean clear) {
        final int size = Util.int32Value(sizeArg);
        final MemoryIO io = factory.allocateHeapMemory(size, clear);
        return new Buffer(context.getRuntime(), recv, io, 0, size);
    }
    @JRubyMethod(name = { "alloc_inout", "__alloc_inout", "__alloc_heap_inout", "__alloc_direct_inout" }, meta = true)
    public static Buffer allocateDirect(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        return allocate(context, recv, sizeArg, CLEAR_DEFAULT);
    }
    @JRubyMethod(name = { "alloc_inout", "__alloc_inout", "__alloc_heap_inout", "__alloc_direct_inout" }, meta = true)
    public static Buffer allocateDirect(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, clearArg.isTrue());
    }
    @JRubyMethod(name = { "alloc_in", "__alloc_in", "__alloc_heap_in", "__alloc_direct_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        if (arg instanceof RubyString) {
            final RubyString s = (RubyString) arg;
            final int size = Util.int32Value(s.length());
            final ByteList bl = s.getByteList();
            final MemoryIO io = factory.allocateHeapMemory(size, false);
            io.put(0, bl.unsafeBytes(), bl.begin(), bl.length());
            io.putByte(bl.length(), (byte) 0);
            return new Buffer(context.getRuntime(), recv, io, 0, size);
        } else {
            return allocate(context, recv, arg, CLEAR_DEFAULT);
        }
    }
    @JRubyMethod(name = { "alloc_in", "__alloc_in", "__alloc_heap_in", "__alloc_direct_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, clearArg.isTrue());
    }
    @JRubyMethod(name = { "alloc_out", "__alloc_out", "__alloc_heap_out", "__alloc_direct_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        return allocate(context, recv, sizeArg, CLEAR_DEFAULT);
    }
    @JRubyMethod(name = { "alloc_out", "__alloc_out", "__alloc_heap_out", "__alloc_direct_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, clearArg.isTrue());
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.getRuntime(),
                String.format("#<Buffer size=%d>", size));
    }
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(ThreadContext context, IRubyObject value) {
        return new Buffer(context.getRuntime(), getMetaClass(),
                this, RubyNumeric.fix2long(value));
    }
    @JRubyMethod(name = "put_pointer", required = 2)
    public IRubyObject put_pointer(ThreadContext context, IRubyObject offset, IRubyObject value) {
        if (value instanceof Pointer) {
            getMemoryIO().putMemoryIO(getOffset(offset), ((Pointer) value).getMemoryIO());
        } else if (value.isNil()) {
            getMemoryIO().putAddress(getOffset(offset), 0L);
        } else {
            throw context.getRuntime().newArgumentError("Cannot convert argument to pointer");
        }
        return this;
    }
    ArrayMemoryIO getArrayMemoryIO() {
        return (ArrayMemoryIO) getMemoryIO();
    }
    protected Pointer getPointer(Ruby runtime, long offset) {
        return factory.newPointer(runtime, getMemoryIO().getMemoryIO(this.offset + offset));
    }

}
