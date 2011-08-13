
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.MappedType;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Struct;
import org.jruby.ext.ffi.StructByValue;
import org.jruby.ext.ffi.Type;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;


/**
 * Manages Callback instances for the low level FFI backend.
 */
public class CallbackManager extends org.jruby.ext.ffi.CallbackManager {
    private static final String CALLBACK_ID = "ffi_callback";

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
     * @param runtime The runtime to create the class for
     * @param module The module to place the class in
     *
     * @return The newly created ruby class
     */
    public static RubyClass createCallbackClass(Ruby runtime, RubyModule module) {

        RubyClass cbClass = module.defineClassUnder("Callback", module.getClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        cbClass.defineAnnotatedMethods(NativeCallbackPointer.class);
        cbClass.defineAnnotatedConstants(NativeCallbackPointer.class);

        return cbClass;
    }
    
    public final org.jruby.ext.ffi.Pointer getCallback(Ruby runtime, CallbackInfo cbInfo, Object proc) {
        return proc instanceof RubyObject
                ? getCallback(runtime, cbInfo, (RubyObject) proc)
                : newCallback(runtime, cbInfo, proc);
    }

    /**
     * Gets a Callback object conforming to the signature contained in the
     * <tt>CallbackInfo</tt> for the ruby <tt>Proc</tt> or <tt>Block</tt> instance.
     *
     * @param runtime The ruby runtime the callback is attached to
     * @param cbInfo The signature of the native callback
     * @param proc The ruby object to call when the callback is invoked.
     * @return A native value returned to the native caller.
     */
    public final org.jruby.ext.ffi.Pointer getCallback(Ruby runtime, CallbackInfo cbInfo, RubyObject proc) {
        if (proc instanceof Function) {
            return (Function) proc;
        }

        synchronized (proc) {
            Object existing = proc.getInternalVariable(CALLBACK_ID);
            if (existing instanceof NativeCallbackPointer && ((NativeCallbackPointer) existing).cbInfo == cbInfo) {
                return (NativeCallbackPointer) existing;
            } else if (existing instanceof Map) {
                Map m = (Map) existing;
                NativeCallbackPointer cb = (NativeCallbackPointer) m.get(proc);
                if (cb != null) {
                    return cb;
                }
            }

            NativeCallbackPointer cb = newCallback(runtime, cbInfo, proc);
            
            if (existing == null) {
                ((RubyObject) proc).setInternalVariable(CALLBACK_ID, cb);
            } else {
                Map<CallbackInfo, NativeCallbackPointer> m = existing instanceof Map
                        ? (Map<CallbackInfo, NativeCallbackPointer>) existing
                        : Collections.synchronizedMap(new WeakHashMap<CallbackInfo, NativeCallbackPointer>());
                m.put(cbInfo, cb);
                m.put(((NativeCallbackPointer) existing).cbInfo, (NativeCallbackPointer) existing);
                ((RubyObject) proc).setInternalVariable(CALLBACK_ID, m);
            }

            return cb;
        }
        
    }

    /**
     * Gets a Callback object conforming to the signature contained in the
     * <tt>CallbackInfo</tt> for the ruby <tt>Proc</tt> or <tt>Block</tt> instance.
     *
     * @param runtime The ruby runtime the callback is attached to
     * @param cbInfo The signature of the native callback
     * @param proc The ruby <tt>Block</tt> object to call when the callback is invoked.
     * @return A native value returned to the native caller.
     */
    final NativeCallbackPointer getCallback(Ruby runtime, CallbackInfo cbInfo, Block proc) {
        return newCallback(runtime, cbInfo, proc);
    }

    private final NativeCallbackPointer newCallback(Ruby runtime, CallbackInfo cbInfo, Object proc) {
        NativeFunctionInfo info = getClosureInfo(runtime, cbInfo);
        NativeClosureProxy cbProxy = new NativeClosureProxy(runtime, info, proc);
        Closure.Handle handle = ClosureManager.getInstance().newClosure(cbProxy, info.callContext);
        return new NativeCallbackPointer(runtime, handle, cbInfo, info);
    }

    private final NativeFunctionInfo getClosureInfo(Ruby runtime, CallbackInfo cbInfo) {
        Object info = cbInfo.getProviderCallbackInfo();
        if (info != null && info instanceof NativeFunctionInfo) {
            return (NativeFunctionInfo) info;
        }

        cbInfo.setProviderCallbackInfo(info = newClosureInfo(runtime, cbInfo));

        return (NativeFunctionInfo) info;
    }

    private final NativeFunctionInfo newClosureInfo(Ruby runtime, CallbackInfo cbInfo) {

        Type[] paramTypes = cbInfo.getParameterTypes();
        for (int i = 0; i < paramTypes.length; ++i) {
            if (!isParameterTypeValid(paramTypes[i]) || FFIUtil.getFFIType(paramTypes[i]) == null) {
                throw runtime.newTypeError("invalid callback parameter type: " + paramTypes[i]);
            }
        }

        if (!isReturnTypeValid(cbInfo.getReturnType()) || FFIUtil.getFFIType(cbInfo.getReturnType()) == null) {
            runtime.newTypeError("invalid callback return type: " + cbInfo.getReturnType());
        }

        return new NativeFunctionInfo(runtime, cbInfo.getReturnType(), cbInfo.getParameterTypes(),
                cbInfo.isStdcall() ? CallingConvention.STDCALL : CallingConvention.DEFAULT);
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

    /**
     * Checks if a type is a valid callback return type
     *
     * @param type The type to examine
     * @return <tt>true</tt> if <tt>type</tt> is a valid return type for a callback.
     */
    private static final boolean isReturnTypeValid(Type type) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG:
                case ULONG:
                case LONG_LONG:
                case ULONG_LONG:
                case FLOAT:
                case DOUBLE:
                case POINTER:
                case VOID:
                case BOOL:
                    return true;
            }

        } else if (type instanceof CallbackInfo) {
            return true;

        } else if (type instanceof StructByValue) {
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a type is a valid parameter type for a callback
     *
     * @param type The type to examine
     * @return <tt>true</tt> if <tt>type</tt> is a valid parameter type for a callback.
     */
    private static final boolean isParameterTypeValid(Type type) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG:
                case ULONG:
                case LONG_LONG:
                case ULONG_LONG:
                case FLOAT:
                case DOUBLE:
                case POINTER:
                case STRING:
                case BOOL:
                    return true;
            }
        } else if (type instanceof CallbackInfo) {
            return true;
        
        } else if (type instanceof StructByValue) {
            return true;
        
        } else if (type instanceof MappedType) {
            return isParameterTypeValid(((MappedType) type).getRealType());
        }
        
        return false;
    }
}
