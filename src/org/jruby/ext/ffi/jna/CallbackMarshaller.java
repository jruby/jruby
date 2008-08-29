package org.jruby.ext.ffi.jna;

import com.sun.jna.CallbackProxy;
import com.sun.jna.Pointer;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.ext.ffi.Callback;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Converts a ruby string or java <tt>ByteBuffer</tt> into a native pointer.
 */
final class CallbackMarshaller implements Marshaller {
    private static final Map<IRubyObject, com.sun.jna.Callback> callbackMap = 
            Collections.synchronizedMap(new WeakHashMap<IRubyObject, com.sun.jna.Callback>());
    private final Callback callback;
    private final Class[] paramTypes;
    private final Class returnType;

    public CallbackMarshaller(Callback cb) {
        this.callback = cb;
        NativeType[] nativeParams = cb.getParameterTypes();
        paramTypes = new Class[nativeParams.length];
        for (int i = 0; i < nativeParams.length; ++i) {
            paramTypes[i] = classFor(nativeParams[i]);
            if (paramTypes[i] == null) {
                throw cb.getRuntime().newArgumentError("Invalid callback parameter type: " + nativeParams[i]);
            }
        }
        switch (cb.getReturnType()) {
            case INT8:
            case UINT8:
            case INT16:
            case UINT16:
            case INT32:
            case UINT32:
            case LONG:
            case ULONG:
            case INT64:
            case UINT64:
            case FLOAT32:
            case FLOAT64:
            case POINTER:
                this.returnType = classFor(cb.getReturnType());
                break;
            default:
               throw cb.getRuntime().newArgumentError("Invalid callback return type: " + cb.getReturnType()); 
                
        }        
    }

    private static final Class classFor(NativeType type) {
        switch (type) {
            case INT8:
            case UINT8:
                return byte.class;
            case INT16:
            case UINT16:
                return short.class;
            case INT32:
            case UINT32:
                return int.class;
            case INT64:
            case UINT64:
                return long.class;
            case FLOAT32:
                return float.class;
            case FLOAT64:
                return double.class;
            case POINTER:
                return Pointer.class;
            default:
                return null;
        }
    }

    public final Object marshal(Invocation invocation, final IRubyObject parameter) {
        com.sun.jna.Callback cb = callbackMap.get(parameter);
        if (cb != null) {
            return cb;
        }
        cb = new CallbackProxy() {
            final WeakReference<IRubyObject> proc = new WeakReference<IRubyObject>(parameter);
            final Ruby runtime = parameter.getRuntime();
            public Object callback(Object[] args) {
                IRubyObject recv = proc.get();
                if (recv == null) {
                    return 0L;
                }
                ThreadContext context = runtime.getCurrentContext();
                NativeType[] nativeParams = callback.getParameterTypes();
                IRubyObject[] params = new IRubyObject[nativeParams.length];
                for (int i = 0; i < params.length; ++i) {
                    switch (nativeParams[i]) {
                        case INT8:
                            params[i] = runtime.newFixnum((Byte) args[i]);
                            break;
                        case UINT8:
                            params[i] = Util.newUnsigned8(runtime, (Byte) args[i]);
                            break;
                        case INT16:
                            params[i] = runtime.newFixnum((Short) args[i]);
                            break;
                        case UINT16:
                            params[i] = Util.newUnsigned16(runtime, (Short) args[i]);
                            break;
                        case INT32:                        
                            params[i] = runtime.newFixnum((Integer) args[i]);
                            break;
                        case UINT32:
                            params[i] =  Util.newUnsigned32(runtime, (Integer) args[i]);
                            break;
                        case INT64:
                            params[i] = runtime.newFixnum((Long) args[i]);
                            break;
                        case UINT64:
                            params[i] =  Util.newUnsigned64(runtime, (Long) args[i]);
                            break;
                        case FLOAT32:
                            params[i] =  runtime.newFloat((Float) args[i]);
                            break;
                        case FLOAT64:
                            params[i] =  runtime.newFloat((Double) args[i]);
                            break;
                        case POINTER:
                            params[i] = new JNAMemoryPointer(runtime, (Pointer) args[i]);
                            break;
                        default:
                            // Ignore bad parameter types
                            return 0L;
                    }
                }
                IRubyObject retVal = ((RubyProc) recv).call(context, params);
                switch (callback.getReturnType()) {
                    case INT8:
                        return (byte) Util.int8Value(retVal);
                    case UINT8:
                        return (byte) Util.uint8Value(retVal);
                    case INT16:
                        return (short) Util.int16Value(retVal);
                    case UINT16:
                        return (short) Util.uint16Value(retVal);
                    case INT32:
                        return (int) Util.int32Value(retVal);
                    case UINT32:
                        return (int) Util.uint32Value(retVal);
                    case INT64:
                    case UINT64:
                        return (long) Util.int64Value(retVal);
                    case FLOAT32:
                        return (float) Util.floatValue(retVal);
                    case FLOAT64:
                        return (double) Util.doubleValue(retVal);
                    case POINTER:
                        return ((JNAMemoryPointer) retVal).getNativeMemory();
                    default:
                        return Long.valueOf(0);
                }
            }

            public Class[] getParameterTypes() {
                return paramTypes;
            }

            public Class getReturnType() {
                return returnType;
            }
        };
        callbackMap.put(parameter, cb);
        return cb;
    }
}
