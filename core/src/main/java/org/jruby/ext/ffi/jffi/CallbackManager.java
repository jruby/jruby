
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.ext.ffi.AbstractInvoker;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.Type;
import org.jruby.runtime.ThreadContext;

import java.util.Map;
import java.util.WeakHashMap;

import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;


/**
 * Manages Callback instances for the low level FFI backend.
 */
public class CallbackManager extends org.jruby.ext.ffi.CallbackManager {
    private final Map<CallbackInfo, NativeCallbackFactory> factories
            = new WeakHashMap<CallbackInfo, NativeCallbackFactory>();

    /** Holder for the single instance of CallbackManager */
    private static final class SingletonHolder {
        static final CallbackManager INSTANCE = new CallbackManager();
    }

    /** 
     * Gets the singleton instance of CallbackManager
     */
    public static final CallbackManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    /**
     * Creates a Callback class for a ruby runtime
     *
     * @param context the current thread context
     * @param FFI The module to place the class in
     *
     * @return The newly created ruby class
     */
    public static RubyClass createCallbackClass(ThreadContext context, RubyModule FFI) {
        return FFI.defineClassUnder(context, "Callback", FFI.getClass(context, "Pointer"), NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, AbstractInvoker.class).
                defineConstants(context, AbstractInvoker.class);
    }
    
    public final org.jruby.ext.ffi.Pointer getCallback(ThreadContext context, CallbackInfo cbInfo, Object proc) {
        return proc instanceof RubyObject
                ? getCallbackFactory(context.runtime, cbInfo).getCallback((RubyObject) proc)
                : getCallbackFactory(context.runtime, cbInfo).newCallback(proc);
    }

    public final synchronized NativeCallbackFactory getCallbackFactory(Ruby runtime, CallbackInfo cbInfo) {
        NativeCallbackFactory factory = factories.get(cbInfo);
        if (factory == null) {
            factories.put(cbInfo, factory = new NativeCallbackFactory(runtime, cbInfo));
        }

        return factory;
    }

    /**
     */
    final CallbackMemoryIO newClosure(Ruby runtime, Type returnType, Type[] parameterTypes, 
            Object proc, CallingConvention convention) {
        NativeFunctionInfo info = new NativeFunctionInfo(runtime, returnType, parameterTypes, convention);

        final NativeClosureProxy cbProxy = new NativeClosureProxy(runtime, info, proc);
        final Closure.Handle handle = ClosureManager.getInstance().newClosure(cbProxy, info.callContext);
        
        return new CallbackMemoryIO(runtime, handle, proc);
    }

}
