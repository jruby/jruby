
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;
import com.kenai.jffi.Type;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.ext.ffi.BasePointer;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.FFIProvider;
import org.jruby.ext.ffi.InvalidMemoryIO;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

public class CallbackManager extends org.jruby.ext.ffi.CallbackManager {
    private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    private static final class SingletonHolder {
        static final CallbackManager INSTANCE = new CallbackManager();
    }
    public static RubyClass createCallbackClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Callback",
                module.fastGetClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(Callback.class);
        result.defineAnnotatedConstants(Callback.class);

        return result;
    }
    private final Map<Object, Map<CallbackInfo, Callback>> callbackMap =
            new WeakHashMap<Object, Map<CallbackInfo, Callback>>();
    private final Map<CallbackInfo, ClosureInfo> infoMap =
            Collections.synchronizedMap(new WeakHashMap<CallbackInfo, ClosureInfo>());
    public static final CallbackManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    public final org.jruby.ext.ffi.Pointer getCallback(Ruby runtime, CallbackInfo cbInfo, Object proc) {
        Map<CallbackInfo, Callback> map;
        synchronized (callbackMap) {
            map = callbackMap.get(proc);
            if (map != null) {
                Callback cb = map.get(cbInfo);
                if (cb != null) {
                    return cb;
                }
            }
            callbackMap.put(proc, map = Collections.synchronizedMap(new HashMap<CallbackInfo, Callback>(2)));
        }
        ClosureInfo info = infoMap.get(cbInfo);
        if (info == null) {
            CallingConvention convention = "stdcall".equals(null)
                    ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
            info = new ClosureInfo(cbInfo, convention);
            infoMap.put(cbInfo, info);
        }
        
        final CallbackProxy cbProxy = new CallbackProxy(runtime, cbInfo, proc);
        final Closure.Handle handle = ClosureManager.getInstance().newClosure(cbProxy,
                info.ffiReturnType, info.ffiParameterTypes, info.convention);
        Callback cb = new Callback(runtime, handle, cbInfo, proc);
        map.put(cbInfo, cb);
        return cb;
    }
    private static class ClosureInfo {
        private final CallbackInfo cbInfo;
        private final CallingConvention convention;
        private final NativeType[] parameterTypes;
        private final Type[] ffiParameterTypes;
        private final Type ffiReturnType;
        public ClosureInfo(CallbackInfo cbInfo, CallingConvention convention) {
            this.cbInfo = cbInfo;
            this.convention = convention;
            NativeParam[] nativeParams = cbInfo.getParameterTypes();
            ffiParameterTypes = new Type[nativeParams.length];
            parameterTypes = new NativeType[nativeParams.length];
            for (int i = 0; i < nativeParams.length; ++i) {
                if (!(nativeParams[i] instanceof NativeType)) {
                    throw new RuntimeException("Invalid callback parameter type: " + nativeParams[i]);
                }
                switch ((NativeType) nativeParams[i]) {
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
                    case STRING:
                        ffiParameterTypes[i] = getFFIType((NativeType) nativeParams[i]);
                        parameterTypes[i] = (NativeType) nativeParams[i];
                        break;
                    default:
                        throw new RuntimeException("Invalid callback parameter type: " + nativeParams[i]);
                }
            }
            switch (cbInfo.getReturnType()) {
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
                case VOID:
                    this.ffiReturnType = getFFIType(cbInfo.getReturnType());
                    break;
                default:
                   throw cbInfo.getRuntime().newArgumentError("Invalid callback return type: " + cbInfo.getReturnType());

            }
        }
    }
    private static final Type getFFIType(NativeType type) {
        switch (type) {
            case VOID: return com.kenai.jffi.Type.VOID;
            case INT8: return com.kenai.jffi.Type.SINT8;
            case UINT8: return com.kenai.jffi.Type.UINT8;
            case INT16: return com.kenai.jffi.Type.SINT16;
            case UINT16: return com.kenai.jffi.Type.UINT16;
            case INT32: return com.kenai.jffi.Type.SINT32;
            case UINT32: return com.kenai.jffi.Type.UINT32;
            case INT64: return com.kenai.jffi.Type.SINT64;
            case UINT64: return com.kenai.jffi.Type.UINT64;
            case LONG:
                return com.kenai.jffi.Platform.getPlatform().addressSize() == 32
                        ? com.kenai.jffi.Type.SINT32
                        : com.kenai.jffi.Type.SINT64;
            case ULONG:
                return com.kenai.jffi.Platform.getPlatform().addressSize() == 32
                        ? com.kenai.jffi.Type.UINT32
                        : com.kenai.jffi.Type.UINT64;
            case FLOAT32: return com.kenai.jffi.Type.FLOAT;
            case FLOAT64: return com.kenai.jffi.Type.DOUBLE;
            case POINTER: return com.kenai.jffi.Type.POINTER;
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
                return com.kenai.jffi.Type.POINTER;
            case STRING: return com.kenai.jffi.Type.POINTER;
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }
    @JRubyClass(name = "FFI::Callback", parent = "FFI::BasePointer")
    static class Callback extends BasePointer {
        private final CallbackInfo cbInfo;
        private final Object proc;
        
        Callback(Ruby runtime, Closure.Handle handle, CallbackInfo cbInfo, Object proc) {
            super(runtime, FFIProvider.getModule(runtime).fastGetClass("Callback"),
                    new CallbackMemoryIO(runtime, handle), Long.MAX_VALUE);
            this.cbInfo = cbInfo;
            this.proc = proc;
        }
    }
    private static final class CallbackProxy implements Closure {
        private final Ruby runtime;
        private final CallbackInfo cbInfo;
        private final WeakReference<Object> proc;
        private final NativeParam[] parameterTypes;
        private final NativeType returnType;

        CallbackProxy(Ruby runtime, CallbackInfo cbInfo, Object proc) {
            this.runtime = runtime;
            this.cbInfo = cbInfo;
            this.proc = new WeakReference<Object>(proc);
            this.parameterTypes = cbInfo.getParameterTypes();
            this.returnType = cbInfo.getReturnType();
        }
        public void invoke(Closure.Buffer buffer) {
            Object recv = proc.get();
            if (recv == null) {
                buffer.setIntReturn(0);
                return;
            }
            IRubyObject[] params = new IRubyObject[parameterTypes.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = fromNative(runtime, (NativeType) parameterTypes[i], buffer, i);
            }
            IRubyObject retVal;
            if (recv instanceof RubyProc) {
                retVal = ((RubyProc) recv).call(runtime.getCurrentContext(), params);
            } else {
                retVal = ((Block) recv).call(runtime.getCurrentContext(), params);
            }
            setReturnValue(runtime, cbInfo.getReturnType(), buffer, retVal);
        }
    }
    static final class CallbackMemoryIO extends InvalidMemoryIO implements DirectMemoryIO {
        private final Closure.Handle handle;
        public CallbackMemoryIO(Ruby runtime,  Closure.Handle handle) {
            super(runtime);
            this.handle = handle;
        }
        public final long getAddress() {
            return handle.getAddress();
        }
        public final boolean isNull() {
            return false;
        }
        public final boolean isDirect() {
            return true;
        }
    }
    /**
     * Extracts the primitive value from a Ruby object.
     * This is similar to Util.longValue(), except it won't throw exceptions for
     * invalid values.
     *
     * @param value The Ruby object to convert
     * @return a java long value.
     */
    private static final long longValue(IRubyObject value) {
        if (value instanceof RubyNumeric) {
            return ((RubyNumeric) value).getLongValue();
        } else if (value.isNil()) {
            return 0L;
        }
        return 0;
    }

    /**
     * Extracts the primitive value from a Ruby object.
     * This is similar to Util.longValue(), except it won't throw exceptions for
     * invalid values.
     *
     * @param value The Ruby object to convert
     * @return a java long value.
     */
    private static final long addressValue(IRubyObject value) {
        if (value instanceof RubyNumeric) {
            return ((RubyNumeric) value).getLongValue();
        } else if (value instanceof BasePointer) {
            return ((BasePointer) value).getAddress();
        } else if (value.isNil()) {
            return 0L;
        }
        return 0;
    }
    private static final void setReturnValue(Ruby runtime, NativeType type,
            Closure.Buffer buffer, IRubyObject value) {
        switch ((NativeType) type) {
            case VOID:
                break;
            case INT8:
                buffer.setByteReturn((byte) longValue(value)); break;
            case UINT8:
                buffer.setByteReturn((byte) longValue(value)); break;
            case INT16:
                buffer.setShortReturn((short) longValue(value)); break;
            case UINT16:
                buffer.setShortReturn((short) longValue(value)); break;
            case INT32:
                buffer.setIntReturn((int) longValue(value)); break;
            case UINT32:
                buffer.setIntReturn((int) longValue(value)); break;
            case INT64:
                buffer.setLongReturn(Util.int64Value(value)); break;
            case UINT64:
                buffer.setLongReturn(Util.uint64Value(value)); break;
            case FLOAT32:
                buffer.setFloatReturn((float) RubyNumeric.num2dbl(value)); break;
            case FLOAT64:
                buffer.setDoubleReturn(RubyNumeric.num2dbl(value)); break;
            case POINTER:
                buffer.setAddressReturn(addressValue(value)); break;
            default:
        }
    }
    private static final IRubyObject fromNative(Ruby runtime, NativeType type,
            Closure.Buffer buffer, int index) {
        switch (type) {
            case VOID:
                return runtime.getNil();
            case INT8:
                return Util.newSigned8(runtime, buffer.getByte(index));
            case UINT8:
                return Util.newUnsigned8(runtime, buffer.getByte(index));
            case INT16:
                return Util.newSigned16(runtime, buffer.getShort(index));
            case UINT16:
                return Util.newUnsigned16(runtime, buffer.getShort(index));
            case INT32:
                return Util.newSigned32(runtime, buffer.getInt(index));
            case UINT32:
                return Util.newUnsigned32(runtime, buffer.getInt(index));
            case INT64:
                return Util.newSigned64(runtime, buffer.getLong(index));
            case UINT64:
                return Util.newUnsigned64(runtime, buffer.getLong(index));
            case FLOAT32:
                return runtime.newFloat(buffer.getFloat(index));
            case FLOAT64:
                return runtime.newFloat(buffer.getDouble(index));
            case POINTER: {
                final long  address = buffer.getAddress(index);
                return new BasePointer(runtime, address != 0 ? new NativeMemoryIO(address) : new NullMemoryIO(runtime));
            }
            case STRING:
                return getStringParameter(runtime, buffer, index);
            default:
                throw new IllegalArgumentException("Invalid type " + type);
        }
    }
    private static final IRubyObject getStringParameter(Ruby runtime, Closure.Buffer buffer, int index) {
        long address = buffer.getAddress(index);
        if (address == 0) {
            return runtime.getNil();
        }
        int len = (int) IO.getStringLength(address);
        if (len == 0) {
            return RubyString.newEmptyString(runtime);
        }
        byte[] bytes = new byte[len];
        IO.getByteArray(address, bytes, 0, len);

        RubyString s = RubyString.newStringShared(runtime, bytes);
        s.setTaint(true);
        return s;
    }
}
