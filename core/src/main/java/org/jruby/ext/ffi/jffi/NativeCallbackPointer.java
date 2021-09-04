package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Closure;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.ext.ffi.AbstractInvoker;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.internal.runtime.methods.DynamicMethod;

/**
 * Wrapper around the native callback, to represent it as a ruby object
 */
@JRubyClass(name = "FFI::Callback", parent = "FFI::Pointer")
class NativeCallbackPointer extends AbstractInvoker {
    final CallbackInfo cbInfo;
    final NativeFunctionInfo closureInfo;

    NativeCallbackPointer(Ruby runtime, RubyClass klass, Closure.Handle handle, CallbackInfo cbInfo, NativeFunctionInfo closureInfo) {
        super(runtime, klass,
                cbInfo.getParameterTypes().length, new CallbackMemoryIO(runtime, handle));
        this.cbInfo = cbInfo;
        this.closureInfo = closureInfo;
    }


    void dispose() {
        MemoryIO mem = getMemoryIO();
        if (mem instanceof CallbackMemoryIO) {
            ((CallbackMemoryIO) mem).free();
        }
    }

    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        com.kenai.jffi.Function function = new com.kenai.jffi.Function(getMemoryIO().address(),
                closureInfo.jffiReturnType, closureInfo.jffiParameterTypes);
        return MethodFactory.createDynamicMethod(getRuntime(), module, function,
                closureInfo.returnType, closureInfo.parameterTypes, closureInfo.convention, getRuntime().getNil(), false);
    }
}
