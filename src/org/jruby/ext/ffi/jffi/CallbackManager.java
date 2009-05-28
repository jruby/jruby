
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;
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
import org.jruby.anno.JRubyClass;
import org.jruby.ext.ffi.BasePointer;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.InvalidMemoryIO;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Type;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;


/**
 * Manages Callback instances for the low level FFI backend.
 */
public class CallbackManager extends org.jruby.ext.ffi.CallbackManager {
    private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    private static final int LONG_SIZE = Platform.getPlatform().longSize();

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

        RubyClass cbClass = module.defineClassUnder("Callback", module.fastGetClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        cbClass.defineAnnotatedMethods(Callback.class);
        cbClass.defineAnnotatedConstants(Callback.class);

        return cbClass;
    }
    
    /**
     * A map to keep {@link Callback} instances alive
     * 
     * This maps from either a Proc or Block to another map of
     * CallbackInfo:Callback. This allows the fringe case of a single proc 
     * object being passed as an argument to different functions.
     */
    private final Map<Object, Map<CallbackInfo, Callback>> callbackMap =
            new WeakHashMap<Object, Map<CallbackInfo, Callback>>();

    /** A map of Ruby CallbackInfo to low level JFFI Closure metadata */
    private final Map<CallbackInfo, ClosureInfo> infoMap =
            Collections.synchronizedMap(new WeakHashMap<CallbackInfo, ClosureInfo>());

    /**
     * Gets a Callback object conforming to the signature contained in the
     * <tt>CallbackInfo</tt> for the ruby <tt>Proc</tt> or <tt>Block</tt> instance.
     *
     * @param runtime The ruby runtime the callback is attached to
     * @param cbInfo The signature of the native callback
     * @param proc The ruby <tt>Proc</tt> or <tt>Block</tt> object to call when
     * the callback is invoked.
     * @return A native value returned to the native caller.
     */
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
        
        final CallbackProxy cbProxy = new CallbackProxy(runtime, info, proc);
        final Closure.Handle handle = ClosureManager.getInstance().newClosure(cbProxy,
                info.ffiReturnType, info.ffiParameterTypes, info.convention);
        Callback cb = new Callback(runtime, handle, cbInfo, proc);
        map.put(cbInfo, cb);

        return cb;
    }

    /**
     * Holds the JFFI return type and parameter types to avoid
     */
    private static class ClosureInfo {
        final CallbackInfo cbInfo;
        final CallingConvention convention;
        final Type returnType;
        final Type[] parameterTypes;
        final com.kenai.jffi.Type[] ffiParameterTypes;
        final com.kenai.jffi.Type ffiReturnType;
        
        public ClosureInfo(CallbackInfo cbInfo, CallingConvention convention) {
            this.cbInfo = cbInfo;
            this.convention = convention;

            Type[] paramTypes = cbInfo.getParameterTypes();
            ffiParameterTypes = new com.kenai.jffi.Type[paramTypes.length];

            for (int i = 0; i < paramTypes.length; ++i) {
                if (!isParameterTypeValid(paramTypes[i])) {
                    throw cbInfo.getRuntime().newArgumentError("Invalid callback parameter type: " + paramTypes[i]);
                }
                ffiParameterTypes[i] = FFIUtil.getFFIType(paramTypes[i].getNativeType());
            }

            if (!isReturnTypeValid(cbInfo.getReturnType())) {
                throw cbInfo.getRuntime().newArgumentError("Invalid callback return type: " + cbInfo.getReturnType());
            }

            this.returnType = cbInfo.getReturnType();
            this.parameterTypes = (Type[]) cbInfo.getParameterTypes().clone();
            this.ffiReturnType = FFIUtil.getFFIType(returnType.getNativeType());
            
        }
    }

    /**
     * Wrapper around the native callback, to represent it as a ruby object
     */
    @JRubyClass(name = "FFI::Callback", parent = "FFI::BasePointer")
    static class Callback extends BasePointer {
        private final CallbackInfo cbInfo;
        private final Object proc;
        
        Callback(Ruby runtime, Closure.Handle handle, CallbackInfo cbInfo, Object proc) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("Callback"),
                    new CallbackMemoryIO(runtime, handle), Long.MAX_VALUE);
            this.cbInfo = cbInfo;
            this.proc = proc;
        }
    }

    /**
     * Wraps a ruby proc in a JFFI Closure
     */
    private static final class CallbackProxy implements Closure {
        private final Ruby runtime;
        private final ClosureInfo closureInfo;
        private final CallbackInfo cbInfo;
        private final WeakReference<Object> proc;
        
        CallbackProxy(Ruby runtime, ClosureInfo closureInfo, Object proc) {
            this.runtime = runtime;
            this.closureInfo = closureInfo;
            this.cbInfo = closureInfo.cbInfo;
            this.proc = new WeakReference<Object>(proc);
        }
        public void invoke(Closure.Buffer buffer) {
            Object recv = proc.get();
            if (recv == null) {
                buffer.setIntReturn(0);
                return;
            }
            IRubyObject[] params = new IRubyObject[closureInfo.parameterTypes.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = fromNative(runtime, closureInfo.parameterTypes[i], buffer, i);
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

    /**
     * An implementation of MemoryIO that throws exceptions on any attempt to read/write
     * the callback memory area (which is code).
     *
     * This also keeps the callback alive via the handle member, as long as this
     * CallbackMemoryIO instance is contained in a valid Callback pointer.
     */
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

    /**
     * Converts a ruby return value into a native callback return value.
     *
     * @param runtime The ruby runtime the callback is attached to
     * @param type The ruby type of the return value
     * @param buffer The native parameter buffer
     * @param value The ruby value
     */
    private static final void setReturnValue(Ruby runtime, Type type,
            Closure.Buffer buffer, IRubyObject value) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
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

                case LONG:
                    if (LONG_SIZE == 32) {
                        buffer.setIntReturn((int) longValue(value));
                    } else {
                        buffer.setLongReturn(Util.int64Value(value));
                    }
                    break;

                case ULONG:
                    if (LONG_SIZE == 32) {
                        buffer.setIntReturn((int) longValue(value));
                    } else {
                        buffer.setLongReturn(Util.uint64Value(value));
                    }
                    break;

                case FLOAT32:
                    buffer.setFloatReturn((float) RubyNumeric.num2dbl(value)); break;
                case FLOAT64:
                    buffer.setDoubleReturn(RubyNumeric.num2dbl(value)); break;
                case POINTER:
                    buffer.setAddressReturn(addressValue(value)); break;
                default:
            }
        } else if (type instanceof CallbackInfo) {
            if (value instanceof RubyProc) {
                Pointer cb = Factory.getInstance().getCallbackManager().getCallback(runtime, (CallbackInfo) type, value);
                buffer.setAddressReturn(addressValue(cb));
            } else {
                buffer.setAddressReturn(0L);
            }
        } else {
            buffer.setLongReturn(0L);
        }
    }

    /**
     * Converts a native value into a ruby object.
     *
     * @param runtime The ruby runtime to create the ruby object in
     * @param type The type of the native parameter
     * @param buffer The JFFI Closure parameter buffer.
     * @param index The index of the parameter in the buffer.
     * @return A new Ruby object.
     */
    private static final IRubyObject fromNative(Ruby runtime, Type type,
            Closure.Buffer buffer, int index) {
        switch (type.getNativeType()) {
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

            case LONG:
                return LONG_SIZE == 32
                        ? Util.newSigned32(runtime, buffer.getInt(index))
                        : Util.newSigned64(runtime, buffer.getLong(index));
            case ULONG:
                return LONG_SIZE == 32
                        ? Util.newUnsigned32(runtime, buffer.getInt(index))
                        : Util.newUnsigned64(runtime, buffer.getLong(index));

            case FLOAT32:
                return runtime.newFloat(buffer.getFloat(index));
            case FLOAT64:
                return runtime.newFloat(buffer.getDouble(index));
            case POINTER: {
                final long address = buffer.getAddress(index);
                if (type instanceof CallbackInfo) {
                    CallbackInfo cbInfo = (CallbackInfo) type;
                    if (address != 0) {
                        return new JFFIInvoker(runtime, address,
                                cbInfo.getReturnType(), cbInfo.getParameterTypes());
                    } else {
                        return runtime.getNil();
                    }
                } else {
                    return new BasePointer(runtime, NativeMemoryIO.wrap(runtime, address));
                }
            }
            case STRING:
                return getStringParameter(runtime, buffer, index);
            default:
                throw new IllegalArgumentException("Invalid type " + type);
        }
    }

    /**
     * Converts a native string value into a ruby string object.
     *
     * @param runtime The ruby runtime to create the ruby string in
     * @param buffer The JFFI Closure parameter buffer.
     * @param index The index of the parameter in the buffer.
     * @return A new Ruby string object or nil if string is NULL.
     */
    private static final IRubyObject getStringParameter(Ruby runtime, Closure.Buffer buffer, int index) {
        return FFIUtil.getString(runtime, buffer.getAddress(index));
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
                    return true;
            }
        } else if (type instanceof CallbackInfo) {
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
                    return true;
            }
        } else if (type instanceof CallbackInfo) {
            return true;
        }
        return false;
    }
}
