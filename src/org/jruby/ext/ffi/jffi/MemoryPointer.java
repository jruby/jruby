
package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.*;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::MemoryPointer", parent = FFIProvider.MODULE_NAME + "::" + AbstractMemoryPointer.className)
public class MemoryPointer extends BasePointer {
    
    public static RubyClass createMemoryPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("MemoryPointer",
                module.getClass(BasePointer.BASE_POINTER_NAME),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(MemoryPointer.class);
        result.defineAnnotatedConstants(MemoryPointer.class);

        return result;
    }

    private MemoryPointer(Ruby runtime, IRubyObject klass, DirectMemoryIO io, long offset, long size) {
        super(runtime, (RubyClass) klass, io, offset, size);
    }
    
    @JRubyMethod(name = { "allocate", "allocate_direct", "allocateDirect" }, meta = true)
    public static MemoryPointer allocateDirect(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        int size = Util.int32Value(sizeArg);
        DirectMemoryIO io = DirectMemoryIO.allocate(size > 0 ? size : 1, false);
        if (io == null) {
            Ruby runtime = context.getRuntime();
            throw new RaiseException(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d bytes", size), true);
        }
        return new MemoryPointer(context.getRuntime(), recv, io, 0, size);
    }
    @JRubyMethod(name = { "allocate", "allocate_direct", "allocateDirect" }, meta = true)
    public static MemoryPointer allocateDirect(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject clearArg) {
        int size = Util.int32Value(sizeArg);
        DirectMemoryIO io = DirectMemoryIO.allocate(size > 0 ? size : 1, clearArg.isTrue());
        if (io == null) {
            Ruby runtime = context.getRuntime();
            throw new RaiseException(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d bytes", size), true);
        }
        return new MemoryPointer(context.getRuntime(), recv, io, 0, size);
    }

    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        return RubyString.newString(context.getRuntime(),
                String.format("MemoryPointer[address=%#x size=%d]", getAddress(), size));
    }

    @Override
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.getRuntime(),
                String.format("#<MemoryPointer address=%#x size=%d>", getAddress(), size));
    }

    @JRubyMethod(name = "free")
    public IRubyObject free(ThreadContext context) {
        ((DirectMemoryIO) getMemoryIO()).free();
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "autorelease=", required = 1)
    public IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        ((DirectMemoryIO) getMemoryIO()).autorelease(release.isTrue());
        return context.getRuntime().getNil();
    }
}
