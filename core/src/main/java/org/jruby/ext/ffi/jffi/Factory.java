
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Factory extends org.jruby.ext.ffi.Factory {

    public Factory() {
        if (!com.kenai.jffi.Platform.getPlatform().isSupported()) {
            throw new UnsatisfiedLinkError("JFFI backend not available");
        }
    }

    @Override
    public void init(Ruby runtime, RubyModule ffi) {
        super.init(runtime, ffi);

        synchronized (ffi) {
            if (ffi.getClass("DynamicLibrary") == null) {
                DynamicLibrary.createDynamicLibraryClass(runtime, ffi);
            }
            if (ffi.getClass("Invoker") == null) {
                JFFIInvoker.createInvokerClass(runtime, ffi);
            }
            if (ffi.getClass("VariadicInvoker") == null) {
                VariadicInvoker.createVariadicInvokerClass(runtime, ffi);
            }
            if (ffi.getClass("Callback") == null) {
                CallbackManager.createCallbackClass(runtime, ffi);
            }
            if (ffi.getClass("Function") == null) {
                Function.createFunctionClass(runtime, ffi);
            }
            if (ffi.getClass("LastError") == null) {
                ffi.defineModuleUnder("LastError").defineAnnotatedMethods(LastError.class);
            }
        }

        runtime.setFFI(new FFI(ffi));
    }

    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param clear If the memory should be cleared.
     * @return A new <tt>MemoryIO</tt>.
     */
    public MemoryIO allocateDirectMemory(Ruby runtime, int size, boolean clear) {
        return CachingNativeMemoryAllocator.allocateAligned(runtime, size, 8, clear);
    }

    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param align The minimum alignment of the memory
     * @param clear If the memory should be cleared.
     * @return A new <tt>MemoryIO</tt>.
     */
    public MemoryIO allocateDirectMemory(Ruby runtime, int size, int align, boolean clear) {
        return CachingNativeMemoryAllocator.allocateAligned(runtime, size, align, clear);
    }

    public MemoryIO allocateTransientDirectMemory(Ruby runtime, int size, int align, boolean clear) {
        return TransientNativeMemoryIO.allocateAligned(runtime, size, align, clear);
    }

    public MemoryIO wrapDirectMemory(Ruby runtime, long address) {
        return NativeMemoryIO.wrap(runtime, address);
    }

    @Override
    public Function newFunction(Ruby runtime, Pointer address, CallbackInfo cbInfo) {
        CodeMemoryIO mem = new CodeMemoryIO(runtime, address);
        RubyClass klass = runtime.getModule("FFI").getClass("Function");
        return new Function(runtime, klass, mem, 
                cbInfo.getReturnType(), cbInfo.getParameterTypes(),
                cbInfo.isStdcall() ? CallingConvention.STDCALL : CallingConvention.DEFAULT, null, false);
    }


    @Override
    public CallbackManager getCallbackManager() {
        return CallbackManager.getInstance();
    }

    private static final com.kenai.jffi.Type getType(NativeType type) {
        com.kenai.jffi.Type jffiType = FFIUtil.getFFIType(type);
        if (jffiType == null) {
            throw new UnsupportedOperationException("Cannot determine native type for " + type);
        }

        return jffiType;
    }

    public int sizeOf(NativeType type) {
        return getType(type).size();
    }

    public int alignmentOf(NativeType type) {
        return getType(type).alignment();
    }

    public static final class LastError {
        @JRubyMethod(name = {  "error" }, module = true)
        public static final  IRubyObject error(ThreadContext context, IRubyObject recv) {
            return context.runtime.newFixnum(com.kenai.jffi.LastError.getInstance().get());
        }

        @JRubyMethod(name = {  "error=" }, module = true)
        public static final  IRubyObject error_set(ThreadContext context, IRubyObject recv, IRubyObject value) {
            com.kenai.jffi.LastError.getInstance().set((int)value.convertToInteger().getLongValue());

            return value;
        }
    }
}
