package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.ClosurePool;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.api.Access;
import org.jruby.ext.ffi.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.WeakIdentityHashMap;

import static org.jruby.api.Error.typeError;

/**
 * 
 */
public class NativeCallbackFactory {
    private final WeakIdentityHashMap closures = new WeakIdentityHashMap();
    private final Ruby runtime;
    private final ClosurePool closurePool;
    private final NativeFunctionInfo closureInfo;
    private final CallbackInfo callbackInfo;
    private final RubyClass callbackClass;
    private final CachingCallSite callSite = new FunctionalCachingCallSite("call");

    public NativeCallbackFactory(Ruby runtime, CallbackInfo cbInfo) {
        var context = runtime.getCurrentContext();
        this.runtime = runtime;
        this.closureInfo = newFunctionInfo(runtime, cbInfo);
        this.closurePool = com.kenai.jffi.ClosureManager.getInstance().getClosurePool(closureInfo.callContext);
        this.callbackInfo = cbInfo;
        this.callbackClass = Access.getClass(context, "FFI", "Callback");
    }

    public final Pointer getCallback(RubyObject callable) {
        return getCallback(callable, callSite);
    }

    public final Pointer getCallback(IRubyObject callable, CachingCallSite callSite) {
        if (callable instanceof Pointer pointer) return pointer;

        Object ffiHandle = callable.getMetaClass().getRealClass().getVariableTableManager().getFFIHandleAccessorForRead().get(callable);
        NativeCallbackPointer cbptr;
        if (ffiHandle instanceof NativeCallbackPointer && ((cbptr = (NativeCallbackPointer) ffiHandle).cbInfo == callbackInfo)) {
            return cbptr;
        }

        return getCallbackPointer(callable, callSite);
    }

    private synchronized Pointer getCallbackPointer(IRubyObject callable, CachingCallSite callSite) {
        NativeCallbackPointer cbptr = (NativeCallbackPointer) closures.get(callable);
        if (cbptr != null) return cbptr;

        closures.put(callable, cbptr = newCallback(callable, callSite));

        if (callable.getMetaClass().getFFIHandleAccessorForRead().get(callable) == null) {
            callable.getMetaClass().getFFIHandleAccessorForWrite().set(callable, cbptr);
        }

        return cbptr;
    }

    NativeCallbackPointer newCallback(IRubyObject callable, CachingCallSite callSite) {
        if (callSite.retrieveCache(callable).method.isUndefined()) {
            throw runtime.newArgumentError("callback does not respond to :" + callSite.getMethodName());
        }

        return new NativeCallbackPointer(runtime, callbackClass,
                closurePool.newClosureHandle(new NativeClosureProxy(runtime, closureInfo, callable, callSite)),
                callbackInfo, closureInfo);
    }

    NativeCallbackPointer newCallback(Object callable) {
        return new NativeCallbackPointer(runtime, callbackClass,
                closurePool.newClosureHandle(new NativeClosureProxy(runtime, closureInfo, callable, callSite)),
                callbackInfo, closureInfo);
    }

    private final NativeFunctionInfo newFunctionInfo(Ruby runtime, CallbackInfo cbInfo) {

        org.jruby.ext.ffi.Type[] paramTypes = cbInfo.getParameterTypes();
        for (int i = 0; i < paramTypes.length; ++i) {
            if (!isParameterTypeValid(paramTypes[i]) || FFIUtil.getFFIType(paramTypes[i]) == null) {
                throw typeError(runtime.getCurrentContext(), "invalid callback parameter type: " + paramTypes[i]);
            }
        }

        if (!isReturnTypeValid(cbInfo.getReturnType()) || FFIUtil.getFFIType(cbInfo.getReturnType()) == null) {
            throw typeError(runtime.getCurrentContext(), "invalid callback return type: " + cbInfo.getReturnType());
        }

        return new NativeFunctionInfo(runtime, cbInfo.getReturnType(), cbInfo.getParameterTypes(),
                cbInfo.isStdcall() ? CallingConvention.STDCALL : CallingConvention.DEFAULT);
    }


    /**
     * Checks if a type is a valid callback return type
     *
     * @param type The type to examine
     * @return <code>true</code> if <code>type</code> is a valid return type for a callback.
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
//                case LONGDOUBLE:
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
     * @return <code>true</code> if <code>type</code> is a valid parameter type for a callback.
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
//                case LONGDOUBLE:
                case FLOAT:
                case DOUBLE:
                case POINTER:
                case STRING:
                case TRANSIENT_STRING:
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
