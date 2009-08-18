
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Pointer;
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
        //
        // Hook up the MemoryPointer class if its not already there
        //
        synchronized (ffi) {
            if (ffi.fastGetClass("DynamicLibrary") == null) {
                DynamicLibrary.createDynamicLibraryClass(runtime, ffi);
            }
            if (ffi.fastGetClass("Invoker") == null) {
                JFFIInvoker.createInvokerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("VariadicInvoker") == null) {
                VariadicInvoker.createVariadicInvokerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("Callback") == null) {
                CallbackManager.createCallbackClass(runtime, ffi);
            }
            if (ffi.fastGetClass("Function") == null) {
                Function.createFunctionClass(runtime, ffi);
            }
            if (ffi.fastGetClass("LastError") == null) {
                ffi.defineModuleUnder("LastError").defineAnnotatedMethods(LastError.class);
            }
        }
    }

    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param clear If the memory should be cleared.
     * @return A new <tt>MemoryIO</tt>.
     */
    public AllocatedDirectMemoryIO allocateDirectMemory(Ruby runtime, int size, boolean clear) {
        return AllocatedNativeMemoryIO.allocate(runtime, size, clear);
    }

    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param align The minimum alignment of the memory
     * @param clear If the memory should be cleared.
     * @return A new <tt>MemoryIO</tt>.
     */
    public AllocatedDirectMemoryIO allocateDirectMemory(Ruby runtime, int size, int align, boolean clear) {
        return AllocatedNativeMemoryIO.allocateAligned(runtime, size, align, clear);
    }

    public DirectMemoryIO wrapDirectMemory(Ruby runtime, long address) {
        return NativeMemoryIO.wrap(runtime, address);
    }

    @Override
    public Function newFunction(Ruby runtime, Pointer address, CallbackInfo cbInfo) {
        CodeMemoryIO mem = new CodeMemoryIO(runtime, address);
        RubyClass klass = runtime.fastGetModule("FFI").fastGetClass("Function");
        return new Function(runtime, klass, mem, 
                cbInfo.getReturnType(), cbInfo.getParameterTypes(), CallingConvention.DEFAULT, null);
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
        @JRubyMethod(name = {  "error" }, meta = true)
        public static final  IRubyObject error(ThreadContext context, IRubyObject recv) {
            return context.getRuntime().newFixnum(com.kenai.jffi.LastError.getInstance().get());
        }
    }
}
