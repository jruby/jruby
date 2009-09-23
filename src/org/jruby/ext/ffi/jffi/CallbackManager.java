
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.anno.JRubyClass;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.InvalidMemoryIO;
import org.jruby.ext.ffi.MemoryIO;
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
    private static final int LONG_SIZE = Platform.getPlatform().longSize();
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

        RubyClass cbClass = module.defineClassUnder("Callback", module.fastGetClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        cbClass.defineAnnotatedMethods(Callback.class);
        cbClass.defineAnnotatedConstants(Callback.class);

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
            Object existing = proc.fastGetInternalVariable(CALLBACK_ID);
            if (existing instanceof Callback && ((Callback) existing).cbInfo == cbInfo) {
                return (Callback) existing;
            } else if (existing instanceof Map) {
                Map m = (Map) existing;
                Callback cb = (Callback) m.get(proc);
                if (cb != null) {
                    return cb;
                }
            }

            Callback cb = newCallback(runtime, cbInfo, proc);
            
            if (existing == null) {
                ((RubyObject) proc).fastSetInternalVariable(CALLBACK_ID, cb);
            } else {
                Map<CallbackInfo, Callback> m = existing instanceof Map
                        ? (Map<CallbackInfo, Callback>) existing
                        : Collections.synchronizedMap(new WeakHashMap<CallbackInfo, Callback>());
                m.put(cbInfo, cb);
                m.put(((Callback) existing).cbInfo, (Callback) existing);
                ((RubyObject) proc).fastSetInternalVariable(CALLBACK_ID, m);
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
    final Callback getCallback(Ruby runtime, CallbackInfo cbInfo, Block proc) {
        return newCallback(runtime, cbInfo, proc);
    }

    private final Callback newCallback(Ruby runtime, CallbackInfo cbInfo, Object proc) {
        ClosureInfo info = getClosureInfo(runtime, cbInfo);
        WeakRefCallbackProxy cbProxy = new WeakRefCallbackProxy(runtime, info, proc);
        Closure.Handle handle = ClosureManager.getInstance().newClosure(cbProxy,
                info.ffiReturnType, info.ffiParameterTypes, info.convention);
        return new Callback(runtime, handle, cbInfo);
    }

    private final ClosureInfo getClosureInfo(Ruby runtime, CallbackInfo cbInfo) {
        Object info = cbInfo.getProviderCallbackInfo();
        if (info != null && info instanceof ClosureInfo) {
            return (ClosureInfo) info;
        }
        cbInfo.setProviderCallbackInfo(info = newClosureInfo(runtime, cbInfo));
        return (ClosureInfo) info;
    }

    private final ClosureInfo newClosureInfo(Ruby runtime, CallbackInfo cbInfo) {
        return new ClosureInfo(runtime, cbInfo.getReturnType(), cbInfo.getParameterTypes(), CallingConvention.DEFAULT);
    }

    /**
     */
    final CallbackMemoryIO newClosure(Ruby runtime, Type returnType, Type[] parameterTypes, 
            Object proc, CallingConvention convention) {
        ClosureInfo info = new ClosureInfo(runtime, returnType, parameterTypes, convention);

        final CallbackProxy cbProxy = new CallbackProxy(runtime, info, proc);
        final Closure.Handle handle = ClosureManager.getInstance().newClosure(cbProxy,
                info.ffiReturnType, info.ffiParameterTypes, info.convention);
        
        return new CallbackMemoryIO(runtime, handle);
    }
    
    /**
     * Holds the JFFI return type and parameter types to avoid
     */
    private static class ClosureInfo {
        final CallingConvention convention;
        final Type returnType;
        final Type[] parameterTypes;
        final com.kenai.jffi.Type[] ffiParameterTypes;
        final com.kenai.jffi.Type ffiReturnType;
        
        public ClosureInfo(Ruby runtime, Type returnType, Type[] paramTypes, CallingConvention convention) {
            this.convention = convention;

            ffiParameterTypes = new com.kenai.jffi.Type[paramTypes.length];

            for (int i = 0; i < paramTypes.length; ++i) {
                if (!isParameterTypeValid(paramTypes[i])) {
                    throw runtime.newArgumentError("Invalid callback parameter type: " + paramTypes[i]);
                }
                ffiParameterTypes[i] = FFIUtil.getFFIType(paramTypes[i].getNativeType());
            }

            if (!isReturnTypeValid(returnType)) {
                runtime.newArgumentError("Invalid callback return type: " + returnType);
            }

            this.returnType = returnType;
            this.parameterTypes = (Type[]) paramTypes.clone();
            this.ffiReturnType = FFIUtil.getFFIType(returnType.getNativeType());
            
        }
    }

    /**
     * Wrapper around the native callback, to represent it as a ruby object
     */
    @JRubyClass(name = "FFI::Callback", parent = "FFI::Pointer")
    static class Callback extends Pointer {
        private final CallbackInfo cbInfo;
        
        Callback(Ruby runtime, Closure.Handle handle, CallbackInfo cbInfo) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("Callback"),
                    new CallbackMemoryIO(runtime, handle), Long.MAX_VALUE);
            this.cbInfo = cbInfo;
        }

        void dispose() {
            MemoryIO mem = getMemoryIO();
            if (mem instanceof CallbackMemoryIO) {
                ((CallbackMemoryIO) mem).free();
            }
        }
    }

    /**
     * Wraps a ruby proc in a JFFI Closure
     */
    private static abstract class AbstractCallbackProxy implements Closure {
        protected final Ruby runtime;
        protected final ClosureInfo closureInfo;
        
        AbstractCallbackProxy(Ruby runtime, ClosureInfo closureInfo) {
            this.runtime = runtime;
            this.closureInfo = closureInfo;
        }

        protected final void invoke(Closure.Buffer buffer, Object recv) {
            IRubyObject[] params = new IRubyObject[closureInfo.parameterTypes.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = fromNative(runtime, closureInfo.parameterTypes[i], buffer, i);
            }
            IRubyObject retVal;
            if (recv instanceof RubyProc) {
                retVal = ((RubyProc) recv).call(runtime.getCurrentContext(), params);
            } else if (recv instanceof Block) {
                retVal = ((Block) recv).call(runtime.getCurrentContext(), params);
            } else {
                retVal = ((IRubyObject) recv).callMethod(runtime.getCurrentContext(), "call", params);
            }

            setReturnValue(runtime, closureInfo.returnType, buffer, retVal);
        }
    }

    /**
     * Wraps a ruby proc in a JFFI Closure
     */
    private static final class WeakRefCallbackProxy extends AbstractCallbackProxy implements Closure {
        private final WeakReference<Object> proc;

        WeakRefCallbackProxy(Ruby runtime, ClosureInfo closureInfo, Object proc) {
            super(runtime, closureInfo);
            this.proc = new WeakReference<Object>(proc);
        }
        public void invoke(Closure.Buffer buffer) {
            Object recv = proc.get();
            if (recv == null) {
                buffer.setIntReturn(0);
                return;
            }
            invoke(buffer, recv);
        }
    }

    /**
     * Wraps a ruby proc in a JFFI Closure
     */
    private static final class CallbackProxy extends AbstractCallbackProxy implements Closure {
        private final Object proc;

        CallbackProxy(Ruby runtime, ClosureInfo closureInfo, Object proc) {
            super(runtime, closureInfo);
            this.proc = proc;
        }

        public void invoke(Closure.Buffer buffer) {
            invoke(buffer, proc);
        }
    }

    /**
     * An implementation of MemoryIO that throws exceptions on any attempt to read/write
     * the callback memory area (which is code).
     *
     * This also keeps the callback alive via the handle member, as long as this
     * CallbackMemoryIO instance is contained in a valid Callback pointer.
     */
    static final class CallbackMemoryIO extends InvalidMemoryIO implements AllocatedDirectMemoryIO {
        private final Closure.Handle handle;
        private final AtomicBoolean released = new AtomicBoolean(false);

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

        public void free() {
            if (released.getAndSet(true)) {
                throw runtime.newRuntimeError("callback already freed");
            }
            handle.free();
        }

        public void setAutoRelease(boolean autorelease) {
            handle.setAutoRelease(autorelease);
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
        } else if (value instanceof Pointer) {
            return ((Pointer) value).getAddress();
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
                case CHAR:
                    buffer.setByteReturn((byte) longValue(value)); break;
                case UCHAR:
                    buffer.setByteReturn((byte) longValue(value)); break;
                case SHORT:
                    buffer.setShortReturn((short) longValue(value)); break;
                case USHORT:
                    buffer.setShortReturn((short) longValue(value)); break;
                case INT:
                    buffer.setIntReturn((int) longValue(value)); break;
                case UINT:
                    buffer.setIntReturn((int) longValue(value)); break;
                case LONG_LONG:
                    buffer.setLongReturn(Util.int64Value(value)); break;
                case ULONG_LONG:
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

                case FLOAT:
                    buffer.setFloatReturn((float) RubyNumeric.num2dbl(value)); break;
                case DOUBLE:
                    buffer.setDoubleReturn(RubyNumeric.num2dbl(value)); break;
                case POINTER:
                    buffer.setAddressReturn(addressValue(value)); break;

                case BOOL:
                    buffer.setIntReturn(value.isTrue() ? 1 : 0); break;
                default:
            }
        } else if (type instanceof CallbackInfo) {
            if (value instanceof RubyProc || value.respondsTo("call")) {
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
            case CHAR:
                return Util.newSigned8(runtime, buffer.getByte(index));
            case UCHAR:
                return Util.newUnsigned8(runtime, buffer.getByte(index));
            case SHORT:
                return Util.newSigned16(runtime, buffer.getShort(index));
            case USHORT:
                return Util.newUnsigned16(runtime, buffer.getShort(index));
            case INT:
                return Util.newSigned32(runtime, buffer.getInt(index));
            case UINT:
                return Util.newUnsigned32(runtime, buffer.getInt(index));
            case LONG_LONG:
                return Util.newSigned64(runtime, buffer.getLong(index));
            case ULONG_LONG:
                return Util.newUnsigned64(runtime, buffer.getLong(index));

            case LONG:
                return LONG_SIZE == 32
                        ? Util.newSigned32(runtime, buffer.getInt(index))
                        : Util.newSigned64(runtime, buffer.getLong(index));
            case ULONG:
                return LONG_SIZE == 32
                        ? Util.newUnsigned32(runtime, buffer.getInt(index))
                        : Util.newUnsigned64(runtime, buffer.getLong(index));

            case FLOAT:
                return runtime.newFloat(buffer.getFloat(index));
            case DOUBLE:
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
                    return new Pointer(runtime, NativeMemoryIO.wrap(runtime, address));
                }
            }
            case STRING:
                return getStringParameter(runtime, buffer, index);

            case BOOL:
                return runtime.newBoolean(buffer.getInt(index) != 0);

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
        }
        return false;
    }
}
