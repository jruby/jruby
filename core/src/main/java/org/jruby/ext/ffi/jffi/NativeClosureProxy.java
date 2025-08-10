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

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.toDouble;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Error.typeError;

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
            params[i] = fromNative(context, closureInfo.parameterTypes[i], buffer, i);
        }

        IRubyObject retVal = recv instanceof Block
                ? ((Block) recv).call(context, params)
                : callSite.call(context, (IRubyObject) recv, (IRubyObject) recv, params);

        setReturnValue(context, closureInfo.returnType, buffer, retVal);
    }


    /**
     * Extracts the primitive value from a Ruby object.
     * This is similar to Util.longValue(), except it won't throw exceptions for
     * invalid values.
     *
     * @param value The Ruby object to convert
     * @return a java long value.
     */
    private static final long longValue(ThreadContext context, IRubyObject value) {
        return value instanceof RubyNumeric num ? num.asLong(context) : 0L;
    }

    /**
     * Extracts the primitive value from a Ruby object.
     * This is similar to Util.longValue(), except it won't throw exceptions for
     * invalid values.
     *
     * @param value The Ruby object to convert
     * @return a java long value.
     */
    private static final long addressValue(ThreadContext context, IRubyObject value) {
        if (value instanceof RubyNumeric num) {
            return num.asLong(context);
        } else if (value instanceof Pointer) {
            return ((Pointer) value).getAddress();
        }
        return 0;
    }

    /**
     * Converts a ruby return value into a native callback return value.
     *
     * @param context The thread context
     * @param type The ruby type of the return value
     * @param buffer The native parameter buffer
     * @param value The ruby value
     */
    private static final void setReturnValue(ThreadContext context, Type type,
            Closure.Buffer buffer, IRubyObject value) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case VOID:
                    break;
                case CHAR, UCHAR:
                    buffer.setByteReturn((byte) longValue(context, value)); break;
                case SHORT, USHORT:
                    buffer.setShortReturn((short) longValue(context, value)); break;
                case INT, UINT:
                    buffer.setIntReturn((int) longValue(context, value)); break;
                case LONG_LONG:
                    buffer.setLongReturn(Util.int64Value(value)); break;
                case ULONG_LONG:
                    buffer.setLongReturn(Util.uint64Value(value)); break;
                case LONG:
                    if (LONG_SIZE == 32) {
                        buffer.setIntReturn((int) longValue(context, value));
                    } else {
                        buffer.setLongReturn(Util.int64Value(value));
                    }
                    break;
                case ULONG:
                    if (LONG_SIZE == 32) {
                        buffer.setIntReturn((int) longValue(context, value));
                    } else {
                        buffer.setLongReturn(Util.uint64Value(value));
                    }
                    break;
                case FLOAT:
                    buffer.setFloatReturn((float) toDouble(context, value)); break;
                case DOUBLE:
                    buffer.setDoubleReturn(toDouble(context, value)); break;
//                case LONGDOUBLE:
//                    break; // not implemented
                case POINTER:
                    buffer.setAddressReturn(addressValue(context, value)); break;

                case BOOL:
                    buffer.setIntReturn(value.isTrue() ? 1 : 0); break;
                default:
            }
        } else if (type instanceof CallbackInfo) {
            if (value instanceof RubyProc || value.respondsTo("call")) {
                Pointer cb = Factory.getInstance().getCallbackManager().getCallback(context, (CallbackInfo) type, value);
                buffer.setAddressReturn(addressValue(context, cb));
            } else {
                buffer.setAddressReturn(0L);
                throw typeError(context, "invalid callback return value, expected Proc or callable object");
            }

        } else if (type instanceof StructByValue) {
            if (value instanceof Struct s) {
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
                        throw runtimeError(context, "size of struct returned from callback too small");
                    }

                    buffer.setStructReturn(arrayMemory.array(), arrayMemory.arrayOffset());

                } else {
                    throw runtimeError(context, "struct return value has illegal backing memory");
                }
            } else if (value.isNil()) {
                // Zero it out
                buffer.setStructReturn(new byte[type.getNativeSize()], 0);

            } else {
                throw typeError(context, value, context.runtime.getFFI().structClass);
            }
        } else if (type instanceof MappedType mappedType) {
            setReturnValue(context, mappedType.getRealType(), buffer, mappedType.toNative(context, value));
        } else {
            buffer.setLongReturn(0L);
            throw runtimeError(context, "unsupported return type from struct: " + type);
        }
    }

    /**
     * Converts a native value into a ruby object.
     *
     * @param context the thread context
     * @param type The type of the native parameter
     * @param buffer The JFFI Closure parameter buffer.
     * @param index The index of the parameter in the buffer.
     * @return A new Ruby object.
     */
    private static final IRubyObject fromNative(ThreadContext context, Type type,
            Closure.Buffer buffer, int index) {
        Ruby runtime = context.runtime;
        if (type instanceof Type.Builtin) {
            return switch (type.getNativeType()) {
                case VOID -> runtime.getNil();
                case CHAR -> Util.newSigned8(runtime, buffer.getByte(index));
                case UCHAR -> Util.newUnsigned8(runtime, buffer.getByte(index));
                case SHORT -> Util.newSigned16(runtime, buffer.getShort(index));
                case USHORT -> Util.newUnsigned16(runtime, buffer.getShort(index));
                case INT -> Util.newSigned32(runtime, buffer.getInt(index));
                case UINT -> Util.newUnsigned32(runtime, buffer.getInt(index));
                case LONG_LONG -> Util.newSigned64(runtime, buffer.getLong(index));
                case ULONG_LONG -> Util.newUnsigned64(runtime, buffer.getLong(index));
                case LONG -> LONG_SIZE == 32
                        ? Util.newSigned32(runtime, buffer.getInt(index))
                        : Util.newSigned64(runtime, buffer.getLong(index));
                case ULONG -> LONG_SIZE == 32
                        ? Util.newUnsigned32(runtime, buffer.getInt(index))
                        : Util.newUnsigned64(runtime, buffer.getLong(index));
                case FLOAT -> runtime.newFloat(buffer.getFloat(index));
                case DOUBLE -> runtime.newFloat(buffer.getDouble(index));
//                case LONGDOUBLE:
//                    return runtime.newFloat(0); // not implemented

                case POINTER -> new Pointer(runtime, NativeMemoryIO.wrap(runtime, buffer.getAddress(index)));
                case STRING, TRANSIENT_STRING -> getStringParameter(runtime, buffer, index);
                case BOOL -> asBoolean(context, buffer.getByte(index) != 0);
                default -> throw typeError(context, "invalid callback parameter type " + type);
            };

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
            return mappedType.fromNative(runtime.getCurrentContext(), fromNative(context, mappedType.getRealType(), buffer, index));

        } else {
            throw typeError(context, "unsupported callback parameter type: " + type);
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
