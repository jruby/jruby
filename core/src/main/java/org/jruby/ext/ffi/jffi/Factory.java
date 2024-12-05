
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.platform.Platform;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asFixnum;

public class Factory extends org.jruby.ext.ffi.Factory {

    public Factory() {
        if (!com.kenai.jffi.Platform.getPlatform().isSupported()) {
            throw new UnsatisfiedLinkError("JFFI backend not available");
        }
    }

    @Override
    public void init(Ruby runtime, RubyModule FFI) {
        super.init(runtime, FFI);

        var context = runtime.getCurrentContext();
        var Object = objectClass(context);

        synchronized (FFI) {
            if (FFI.getClass("DynamicLibrary") == null) DynamicLibrary.createDynamicLibraryClass(context, FFI, Object);
            if (FFI.getClass("Invoker") == null) JFFIInvoker.createInvokerClass(context, FFI);
            if (FFI.getClass("VariadicInvoker") == null) VariadicInvoker.createVariadicInvokerClass(context, FFI, Object);
            if (FFI.getClass("Callback") == null) CallbackManager.createCallbackClass(context, FFI);
            if (FFI.getClass("Function") == null) Function.createFunctionClass(context, FFI);
            if (FFI.getClass("LastError") == null) {
                var LastError = FFI.defineModuleUnder(context, "LastError").defineMethods(context, LastError.class);
                if (Platform.IS_WINDOWS) LastError.defineMethods(context, WinapiLastError.class);
            }
        }

        runtime.setFFI(new FFI(FFI));
    }

    /**
     * Allocates memory on the native C heap and wraps it in a <code>MemoryIO</code> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param clear If the memory should be cleared.
     * @return A new <code>MemoryIO</code>.
     */
    public MemoryIO allocateDirectMemory(Ruby runtime, int size, boolean clear) {
        return CachingNativeMemoryAllocator.allocateAligned(runtime, size, 8, clear);
    }

    /**
     * Allocates memory on the native C heap and wraps it in a <code>MemoryIO</code> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param align The minimum alignment of the memory
     * @param clear If the memory should be cleared.
     * @return A new <code>MemoryIO</code>.
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
        public static final IRubyObject error(ThreadContext context, IRubyObject recv) {
            return asFixnum(context, com.kenai.jffi.LastError.getInstance().get());
        }

        @JRubyMethod(name = {  "error=" }, module = true)
        public static final IRubyObject error_set(ThreadContext context, IRubyObject recv, IRubyObject value) {
            com.kenai.jffi.LastError.getInstance().set((int)value.convertToInteger().getLongValue());

            return value;
        }
    }

    public static final class WinapiLastError {
        @JRubyMethod(name = {  "winapi_error" }, module = true)
        public static final IRubyObject winapi_error(ThreadContext context, IRubyObject recv) {
            return LastError.error(context, recv);
        }

        @JRubyMethod(name = {  "winapi_error=" }, module = true)
        public static final IRubyObject winapi_error_set(ThreadContext context, IRubyObject recv, IRubyObject value) {
            return LastError.error_set(context, recv, value);
        }
    }
}
