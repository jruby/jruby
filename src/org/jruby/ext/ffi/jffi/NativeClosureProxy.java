package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import org.jruby.Ruby;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.ext.ffi.*;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;

import java.lang.ref.WeakReference;

/**
 * Wraps a ruby proc in a JFFI Closure
 */
final class NativeClosureProxy implements Closure {
    private static final int LONG_SIZE = Platform.getPlatform().longSize();
    protected final Ruby runtime;
    protected final NativeFunctionInfo closureInfo;
    private final WeakReference<Object> proc;
    private final CallSite callSite;

    NativeClosureProxy(Ruby runtime, NativeFunctionInfo closureInfo, Object proc) {
        this(runtime, closureInfo, proc,  new FunctionalCachingCallSite("call"));
    }

    NativeClosureProxy(Ruby runtime, NativeFunctionInfo closureInfo, Object proc, CallSite callSite) {
        this.runtime = runtime;
        this.closureInfo = closureInfo;
        this.proc = new WeakReference<Object>(proc);
        this.callSite = callSite;
    }

    public void invoke(Buffer buffer) {
        Object recv = proc.get();
        if (recv == null) {
            buffer.setIntReturn(0);
            return;
        }
        invoke(buffer, recv);
    }

    protected final void invoke(Buffer buffer, Object recv) {
        ThreadContext context = runtime.getCurrentContext();

        IRubyObject[] params = new IRubyObject[closureInfo.parameterTypes.length];
        for (int i = 0; i < params.length; ++i) {
            params[i] = fromNative(runtime, closureInfo.parameterTypes[i], buffer, i);
        }

        IRubyObject retVal = recv instanceof Block
                ? ((Block) recv).call(context, params)
                : callSite.call(context, (IRubyObject) recv, (IRubyObject) recv, params);

        setReturnValue(runtime, closureInfo.returnType, buffer, retVal);
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
                throw runtime.newTypeError("invalid callback return value, expected Proc or callable object");
            }

        } else if (type instanceof StructByValue) {

            if (value instanceof Struct) {
                Struct s = (Struct) value;
                MemoryIO memory = s.getMemory().getMemoryIO();

                if (memory.isDirect()) {
                    long address = memory.address();
                    if (address != 0) {
                        buffer.setStructReturn(address);
                    } else {
                        // Zero it out
                        buffer.setStructReturn(new byte[type.getNativeSize()], 0);
                    }

                } else if (memory instanceof ArrayMemoryIO) {
                    ArrayMemoryIO arrayMemory = (ArrayMemoryIO) memory;
                    if (arrayMemory.arrayLength() < type.getNativeSize()) {
                        throw runtime.newRuntimeError("size of struct returned from callback too small");
                    }

                    buffer.setStructReturn(arrayMemory.array(), arrayMemory.arrayOffset());

                } else {
                    throw runtime.newRuntimeError("struct return value has illegal backing memory");
                }
            } else if (value.isNil()) {
                // Zero it out
                buffer.setStructReturn(new byte[type.getNativeSize()], 0);

            } else {
                throw runtime.newTypeError(value, runtime.getFFI().structClass);
            }

        } else if (type instanceof MappedType) {
            MappedType mappedType = (MappedType) type;
            setReturnValue(runtime, mappedType.getRealType(), buffer, mappedType.toNative(runtime.getCurrentContext(), value));

        } else {
            buffer.setLongReturn(0L);
            throw runtime.newRuntimeError("unsupported return type from struct: " + type);
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
        if (type instanceof Type.Builtin) {
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

                case POINTER:
                    return new Pointer(runtime, NativeMemoryIO.wrap(runtime, buffer.getAddress(index)));

                case STRING:
                case TRANSIENT_STRING:
                    return getStringParameter(runtime, buffer, index);

                case BOOL:
                    return runtime.newBoolean(buffer.getByte(index) != 0);

                default:
                    throw runtime.newTypeError("invalid callback parameter type " + type);
            }

        } else if (type instanceof CallbackInfo) {
            final CallbackInfo cbInfo = (CallbackInfo) type;
            final long address = buffer.getAddress(index);

            return address != 0
                ? new Function(runtime, cbInfo.getMetaClass(),
                    new CodeMemoryIO(runtime, address),
                    cbInfo.getReturnType(), cbInfo.getParameterTypes(),
                    cbInfo.isStdcall() ? CallingConvention.STDCALL : CallingConvention.DEFAULT, runtime.getNil(), false)

                : runtime.getNil();

        } else if (type instanceof StructByValue) {
            StructByValue sbv = (StructByValue) type;
            final long address = buffer.getStruct(index);
            MemoryIO memory = address != 0
                    ? new BoundedNativeMemoryIO(runtime, address, type.getNativeSize())
                    : runtime.getFFI().getNullMemoryIO();

            return sbv.getStructClass().newInstance(runtime.getCurrentContext(),
                        new IRubyObject[] { new Pointer(runtime, memory) },
                        Block.NULL_BLOCK);

        } else if (type instanceof MappedType) {
            MappedType mappedType = (MappedType) type;
            return mappedType.fromNative(runtime.getCurrentContext(), fromNative(runtime, mappedType.getRealType(), buffer, index));

        } else {
            throw runtime.newTypeError("unsupported callback parameter type: " + type);
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

}
