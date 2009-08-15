
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import com.kenai.jffi.InvocationBuffer;
import com.kenai.jffi.Invoker;
import com.kenai.jffi.ArrayFlags;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.Buffer;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.MemoryPointer;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Struct;
import org.jruby.ext.ffi.StructByValue;
import org.jruby.ext.ffi.StructLayout;
import org.jruby.ext.ffi.Type;
import org.jruby.ext.ffi.Util;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;


public final class DefaultMethodFactory {
    private static final class SingletonHolder {
        private static final DefaultMethodFactory INSTANCE = new DefaultMethodFactory();
    }
    private DefaultMethodFactory() {}
    public static final DefaultMethodFactory getFactory() {
        return SingletonHolder.INSTANCE;
    }
    
    DynamicMethod createMethod(RubyModule module, Function function, 
            Type returnType, Type[] parameterTypes, CallingConvention convention, IRubyObject enums) {

        FunctionInvoker functionInvoker = getFunctionInvoker(returnType);

        ParameterMarshaller[] marshallers = new ParameterMarshaller[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i)  {
            marshallers[i] = getMarshaller(parameterTypes[i], convention, enums);
            if (marshallers[i] == null) {
                throw module.getRuntime().newTypeError("Could not create marshaller for " + parameterTypes[i]);
            }
        }

        /*
         * If there is exactly _one_ callback argument to the function,
         * then a block can be given and automatically subsituted for the callback
         * parameter.
         */
        if (marshallers.length > 0) {
            int cbcount = 0, cbindex = -1;
            for (int i = 0; i < marshallers.length; ++i) {
                if (marshallers[i] instanceof CallbackMarshaller) {
                    cbcount++;
                    cbindex = i;
                }
            }
            if (cbcount == 1) {
                return new CallbackMethodWithBlock(module, function, functionInvoker, marshallers, cbindex);
            }
        }

        //
        // Determine if the parameter might be passed as a 32bit int parameter.
        // This just applies to buffer/pointer types.
        //
        FastIntMethodFactory fastIntFactory = FastIntMethodFactory.getFactory();
        boolean canBeFastInt = enums.isNil() && parameterTypes.length <= 3 && fastIntFactory.isFastIntResult(returnType);
        for (int i = 0; canBeFastInt && i < parameterTypes.length; ++i) {
            if (!(parameterTypes[i] instanceof Type.Builtin) || marshallers[i].needsInvocationSession()) {
                canBeFastInt = false;
            } else {
                switch (parameterTypes[i].getNativeType()) {
                    case POINTER:
                    case BUFFER_IN:
                    case BUFFER_OUT:
                    case BUFFER_INOUT:
                        canBeFastInt = Platform.getPlatform().addressSize() == 32;
                        break;
                    default:
                        canBeFastInt = fastIntFactory.isFastIntParam(parameterTypes[i]);
                        break;
                }
            }
        }

        if (!canBeFastInt) switch (parameterTypes.length) {
            case 0:
                return new DefaultMethodZeroArg(module, function, functionInvoker);
            case 1:
                return new DefaultMethodOneArg(module, function, functionInvoker, marshallers);
            case 2:
                return new DefaultMethodTwoArg(module, function, functionInvoker, marshallers);
            case 3:
                return new DefaultMethodThreeArg(module, function, functionInvoker, marshallers);
            default:
                return new DefaultMethod(module, function, functionInvoker, marshallers);
        }
        //
        // Set up for potentially fast-int operations
        //
        
        IntResultConverter resultConverter = fastIntFactory.getIntResultConverter(returnType);
        IntParameterConverter[] intParameterConverters = new IntParameterConverter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i) {
            intParameterConverters[i] = fastIntFactory.getIntParameterConverter(parameterTypes[i]);
        }
        switch (parameterTypes.length) {
            case 0:
                return new FastIntMethodZeroArg(module, function, resultConverter, intParameterConverters);
            case 1:
                return new FastIntPointerMethodOneArg(module, function, resultConverter, 
                        intParameterConverters, marshallers);
            case 2:
                return new FastIntPointerMethodTwoArg(module, function, resultConverter,
                        intParameterConverters, marshallers);
            case 3:
                return new FastIntPointerMethodThreeArg(module, function, resultConverter,
                        intParameterConverters, marshallers);
        }
        throw new IllegalArgumentException("Parameter types not supported");
    }

    static FunctionInvoker getFunctionInvoker(Type returnType) {
        if (returnType instanceof Type.Builtin) {
            return getFunctionInvoker(returnType.getNativeType());
        } else if (returnType instanceof CallbackInfo) {
            return new CallbackInvoker((CallbackInfo) returnType);
        } else if (returnType instanceof org.jruby.ext.ffi.Enum) {
            return new EnumInvoker((org.jruby.ext.ffi.Enum) returnType);
        } else if (returnType instanceof StructByValue) {
            return new StructByValueInvoker((StructByValue) returnType);
        }
        throw returnType.getRuntime().newArgumentError("Cannot get FunctionInvoker for " + returnType);
    }

    static FunctionInvoker getFunctionInvoker(NativeType returnType) {
        switch (returnType) {
            case VOID:
                return VoidInvoker.INSTANCE;
            case BOOL:
                return BooleanInvoker.INSTANCE;
            case POINTER:
                return PointerInvoker.INSTANCE;
            case CHAR:
                return Signed8Invoker.INSTANCE;
            case SHORT:
                return Signed16Invoker.INSTANCE;
            case INT:
                return Signed32Invoker.INSTANCE;
            case UCHAR:
                return Unsigned8Invoker.INSTANCE;
            case USHORT:
                return Unsigned16Invoker.INSTANCE;
            case UINT:
                return Unsigned32Invoker.INSTANCE;
            case LONG_LONG:
                return Signed64Invoker.INSTANCE;
            case ULONG_LONG:
                return Unsigned64Invoker.INSTANCE;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Invoker.INSTANCE
                        : Signed64Invoker.INSTANCE;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? Unsigned32Invoker.INSTANCE
                        : Unsigned64Invoker.INSTANCE;
            case FLOAT:
                return Float32Invoker.INSTANCE;
            case DOUBLE:
                return Float64Invoker.INSTANCE;
            case STRING:
                return StringInvoker.INSTANCE;
            default:
                throw new IllegalArgumentException("Invalid return type: " + returnType);
        }
    }
    /**
     * Gets a marshaller to convert from a ruby type to a native type.
     *
     * @param type The native type to convert to.
     * @return A new <tt>Marshaller</tt>
     */
    static final ParameterMarshaller getMarshaller(Type type, CallingConvention convention, IRubyObject enums) {
        if (type instanceof Type.Builtin) {
            return enums != null && !enums.isNil() ? getEnumMarshaller(type, enums) : getMarshaller(type.getNativeType());
        } else if (type instanceof org.jruby.ext.ffi.CallbackInfo) {
            return new CallbackMarshaller((org.jruby.ext.ffi.CallbackInfo) type, convention);
        } else if (type instanceof org.jruby.ext.ffi.Enum) {
            return getEnumMarshaller(type, type.callMethod(type.getRuntime().getCurrentContext(), "to_hash"));
        } else if (type instanceof org.jruby.ext.ffi.StructByValue) {
            return new StructByValueMarshaller((org.jruby.ext.ffi.StructByValue) type);
        } else {
            return null;
        }
    }

    /**
     * Gets a marshaller to convert from a ruby type to a native type.
     *
     * @param type The native type to convert to.
     * @param enums The enum map
     * @return A new <tt>ParameterMarshaller</tt>
     */
    static final ParameterMarshaller getEnumMarshaller(Type type, IRubyObject enums) {
        switch (type.getNativeType()) {
            case CHAR:
            case UCHAR:
            case SHORT:
            case USHORT:
            case INT:
            case UINT:
            case LONG_LONG:
            case ULONG_LONG:
                if (!(enums instanceof RubyHash)) {
                    throw type.getRuntime().newArgumentError("wrong argument type "
                            + enums.getMetaClass().getName() + " (expected Hash)");
                }
                return new EnumMarshaller(getMarshaller(type.getNativeType()), (RubyHash) enums);
            default:
                return getMarshaller(type.getNativeType());
        }
    }

    /**
     * Gets a marshaller to convert from a ruby type to a native type.
     *
     * @param type The native type to convert to.
     * @return A new <tt>ParameterMarshaller</tt>
     */
    static final ParameterMarshaller getMarshaller(NativeType type) {
        switch (type) {
            case BOOL:
                return BooleanMarshaller.INSTANCE;
            case CHAR:
                return Signed8Marshaller.INSTANCE;
            case UCHAR:
                return Unsigned8Marshaller.INSTANCE;
            case SHORT:
                return Signed16Marshaller.INSTANCE;
            case USHORT:
                return Unsigned16Marshaller.INSTANCE;
            case INT:
                return Signed32Marshaller.INSTANCE;
            case UINT:
                return Unsigned32Marshaller.INSTANCE;
            case LONG_LONG:
                return Signed64Marshaller.INSTANCE;
            case ULONG_LONG:
                return Unsigned64Marshaller.INSTANCE;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Marshaller.INSTANCE
                        : Signed64Marshaller.INSTANCE;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Marshaller.INSTANCE
                        : Unsigned64Marshaller.INSTANCE;
            case FLOAT:
                return Float32Marshaller.INSTANCE;
            case DOUBLE:
                return Float64Marshaller.INSTANCE;
            case STRING:
                return StringMarshaller.INSTANCE;
            case POINTER:
                return BufferMarshaller.INOUT;
            case BUFFER_IN:
                return BufferMarshaller.IN;
            case BUFFER_OUT:
                return BufferMarshaller.OUT;
            case BUFFER_INOUT:
                return BufferMarshaller.INOUT;
            default:
                throw new IllegalArgumentException("Invalid parameter type: " + type);
        }
    }
    
    private static abstract class BaseInvoker implements FunctionInvoker {
        static final Invoker invoker = Invoker.getInstance();
    }
    /**
     * Invokes the native function with no return type, and returns nil to ruby.
     */
    private static final class VoidInvoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            invoker.invokeInt(function, args);
            return runtime.getNil();
        }
        public static final FunctionInvoker INSTANCE = new VoidInvoker();
    }

    /**
     * Invokes the native function with a boolean return value.
     * Returns a Boolean to ruby.
     */
    private static final class BooleanInvoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return runtime.newBoolean(invoker.invokeInt(function, args) != 0);
        }
        public static final FunctionInvoker INSTANCE = new BooleanInvoker();
    }

    /**
     * Invokes the native function with a native int type, and converts to a symbol
     */
    private static final class EnumInvoker extends BaseInvoker {
        private final org.jruby.ext.ffi.Enum returnType;

        public EnumInvoker(org.jruby.ext.ffi.Enum returnType) {
            this.returnType = returnType;
        }

        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return returnType.callMethod(runtime.getCurrentContext(), "find",
                    runtime.newFixnum(invoker.invokeInt(function, args)));
        }
    }

    /**
     * Invokes the native function with n signed 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed8Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newSigned8(runtime, invoker.invokeInt(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Signed8Invoker();
    }

    /**
     * Invokes the native function with an unsigned 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned8Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newUnsigned8(runtime, invoker.invokeInt(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned8Invoker();
    }

    /**
     * Invokes the native function with n signed 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed16Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newSigned16(runtime, invoker.invokeInt(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Signed16Invoker();
    }

    /**
     * Invokes the native function with an unsigned 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned16Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newUnsigned16(runtime, invoker.invokeInt(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned16Invoker();
    }
    /**
     * Invokes the native function with a 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed32Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newSigned32(runtime, invoker.invokeInt(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Signed32Invoker();
    }

    /**
     * Invokes the native function with an unsigned 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned32Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newUnsigned32(runtime, invoker.invokeInt(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned32Invoker();
    }

    /**
     * Invokes the native function with a 64 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed64Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newSigned64(runtime, invoker.invokeLong(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Signed64Invoker();
    }

    /**
     * Invokes the native function with a 64 bit unsigned integer return value.
     * Returns a ruby Fixnum or Bignum.
     */
    private static final class Unsigned64Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return Util.newUnsigned64(runtime, invoker.invokeLong(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned64Invoker();
    }

    /**
     * Invokes the native function with a 32 bit float return value.
     * Returns a Float to ruby.
     */
    private static final class Float32Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return runtime.newFloat(invoker.invokeFloat(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Float32Invoker();
    }

    /**
     * Invokes the native function with a 64 bit float return value.
     * Returns a Float to ruby.
     */
    private static final class Float64Invoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return runtime.newFloat(invoker.invokeDouble(function, args));
        }
        public static final FunctionInvoker INSTANCE = new Float64Invoker();
    }

    /**
     * Invokes the native function with a native pointer return value.
     * Returns a {@link MemoryPointer} to ruby.
     */
    private static final class PointerInvoker extends BaseInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            final long address = invoker.invokeAddress(function, args);
            return new Pointer(runtime, NativeMemoryIO.wrap(runtime, address));
        }
        public static final FunctionInvoker INSTANCE = new PointerInvoker();
    }
    
    /**
     * Invokes the native function with a native string return value.
     * Returns a {@link RubyString} to ruby.
     */
    private static final class StringInvoker extends BaseInvoker {
        private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();

        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return FFIUtil.getString(runtime, invoker.invokeAddress(function, args));
        }
        public static final FunctionInvoker INSTANCE = new StringInvoker();
    }

    /**
     * Invokes the native function with a native struct return value.
     * Returns a FFI::Struct instance to ruby.
     */
    private static final class StructByValueInvoker extends BaseInvoker {
        private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
        private final StructByValue info;

        public StructByValueInvoker(StructByValue info) {
            this.info = info;
        }

        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            return info.newStruct(runtime, invoker.invokeStruct(function, args), 0);
        }
    }

    /**
     * Invokes the native function with a callback/function pointer return value.
     * Returns a {@link Invoker} to ruby.
     */
    private static final class CallbackInvoker extends BaseInvoker {
        private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
        private final Type returnType;
        private final Type[] parameterTypes;

        public CallbackInvoker(CallbackInfo cbInfo) {
            this.returnType = cbInfo.getReturnType();
            this.parameterTypes = cbInfo.getParameterTypes();
        }
        

        public final IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args) {
            long address = invoker.invokeAddress(function, args);
            if (address == 0) {
                return runtime.getNil();
            }
            return new JFFIInvoker(runtime, address, returnType, parameterTypes);
        }
    }

    /*------------------------------------------------------------------------*/
    static abstract class BaseMarshaller implements ParameterMarshaller {
        public boolean needsInvocationSession() {
            return false;
        }
    }
    /**
     * Converts a ruby Enum into an native integer.
     */
    static final class EnumMarshaller extends BaseMarshaller {
        private final ParameterMarshaller marshaller;
        private final RubyHash enums;

        public EnumMarshaller(ParameterMarshaller marshaller, RubyHash enums) {
            this.marshaller = marshaller;
            this.enums = enums;
        }

        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            if (parameter instanceof RubySymbol) {
                parameter = enums.fastARef(parameter);
                if (parameter.isNil()) {
                    throw context.getRuntime().newArgumentError("wrong argument.  Could not locate enum value for " + parameter);
                }
            }
            marshaller.marshal(context, buffer, parameter);
        }
        public void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            marshal(invocation.getThreadContext(), buffer, parameter);
        }
    }

    /**
     * Converts a ruby Boolean into an 32 bit native integer.
     */
    static final class BooleanMarshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putInt(parameter.isTrue() ? 1 : 0);
        }
        public void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            if (!(parameter instanceof RubyBoolean)) {
                throw invocation.getThreadContext().getRuntime().newTypeError("wrong argument type.  Expected true or false");
            }
            buffer.putInt(parameter.isTrue() ? 1 : 0);
        }
        public static final ParameterMarshaller INSTANCE = new BooleanMarshaller();
    }

    /**
     * Converts a ruby Fixnum into an 8 bit native integer.
     */
    static final class Signed8Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putByte(Util.int8Value(parameter));
        }
        public void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putByte(Util.int8Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Signed8Marshaller();
    }

    /**
     * Converts a ruby Fixnum into an 8 bit native unsigned integer.
     */
    static final class Unsigned8Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putByte(Util.uint8Value(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putByte(Util.uint8Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Unsigned8Marshaller();
    }

    /**
     * Converts a ruby Fixnum into a 16 bit native signed integer.
     */
    static final class Signed16Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putShort(Util.int16Value(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putShort(Util.int16Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Signed16Marshaller();
    }

    /**
     * Converts a ruby Fixnum into a 16 bit native unsigned integer.
     */
    static final class Unsigned16Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putShort(Util.uint16Value(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putShort(Util.uint16Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Unsigned16Marshaller();
    }

    /**
     * Converts a ruby Fixnum into a 32 bit native signed integer.
     */
    static final class Signed32Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putInt(Util.int32Value(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putInt(Util.int32Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Signed32Marshaller();
    }

    /**
     * Converts a ruby Fixnum into a 32 bit native unsigned integer.
     */
    static final class Unsigned32Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putInt((int) Util.uint32Value(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putInt((int) Util.uint32Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Unsigned32Marshaller();
    }

    /**
     * Converts a ruby Fixnum into a 64 bit native signed integer.
     */
    static final class Signed64Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putLong(Util.int64Value(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putLong(Util.int64Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Signed64Marshaller();
    }

    /**
     * Converts a ruby Fixnum into a 64 bit native unsigned integer.
     */
    static final class Unsigned64Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putLong(Util.uint64Value(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putLong(Util.uint64Value(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Unsigned64Marshaller();
    }

    /**
     * Converts a ruby Float into a 32 bit native float.
     */
    static final class Float32Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putFloat((float) RubyNumeric.num2dbl(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putFloat((float) RubyNumeric.num2dbl(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Float32Marshaller();
    }

    /**
     * Converts a ruby Float into a 64 bit native float.
     */
    static final class Float64Marshaller extends BaseMarshaller {
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putDouble(RubyNumeric.num2dbl(parameter));
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putDouble(RubyNumeric.num2dbl(parameter));
        }
        public static final ParameterMarshaller INSTANCE = new Float64Marshaller();
    }

    /**
     * Converts a ruby Buffer into a native address.
     */
    static final class BufferMarshaller extends BaseMarshaller {
        static final ParameterMarshaller IN = new BufferMarshaller(ArrayFlags.IN);
        static final ParameterMarshaller OUT = new BufferMarshaller(ArrayFlags.OUT);
        static final ParameterMarshaller INOUT = new BufferMarshaller(ArrayFlags.IN | ArrayFlags.OUT);
        private final int flags;
        public BufferMarshaller(int flags) {
            this.flags = flags;
        }
        private static final int bufferFlags(Buffer buffer) {
            int f = buffer.getInOutFlags();
            return ((f & Buffer.IN) != 0 ? ArrayFlags.IN: 0)
                    | ((f & Buffer.OUT) != 0 ? ArrayFlags.OUT : 0);
        }
        @Override
        public boolean needsInvocationSession() {
            return false;
        }
        private static final void addBufferParameter(InvocationBuffer buffer, IRubyObject parameter, int flags) {
            ArrayMemoryIO memory = (ArrayMemoryIO) ((Buffer) parameter).getMemoryIO();
                buffer.putArray(memory.array(), memory.arrayOffset(), memory.arrayLength(),
                        flags & bufferFlags((Buffer) parameter));
        }
        private static final long getAddress(Pointer ptr) {
            return ((DirectMemoryIO) ptr.getMemoryIO()).getAddress();
        }
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            if (parameter instanceof Buffer) {
                addBufferParameter(buffer, parameter, flags);
            } else if (parameter instanceof Pointer) {
                buffer.putAddress(getAddress((Pointer) parameter));
            } else if (parameter instanceof Struct) {
                IRubyObject memory = ((Struct) parameter).getMemory();
                if (memory instanceof Buffer) {
                    addBufferParameter(buffer, memory, flags);
                } else if (memory instanceof Pointer) {
                    buffer.putAddress(getAddress((Pointer) memory));
                } else if (memory == null || memory.isNil()) {
                    buffer.putAddress(0L);
                } else {
                    throw context.getRuntime().newArgumentError("Invalid Struct memory");
                }
            } else if (parameter.isNil()) {
                buffer.putAddress(0L);
            } else if (parameter instanceof RubyString) {
                ByteList bl = ((RubyString) parameter).getByteList();
                buffer.putArray(bl.unsafeBytes(), bl.begin(), bl.length(), flags | ArrayFlags.NULTERMINATE);

            } else if (parameter.respondsTo("to_ptr")) {
                final int MAXRECURSE = 4;
                for (int depth = 0; depth < MAXRECURSE; ++depth) {
                    IRubyObject ptr = parameter.callMethod(context, "to_ptr");
                    if (ptr instanceof Pointer) {
                        buffer.putAddress(getAddress((Pointer) ptr));
                    } else if (ptr instanceof Buffer) {
                        addBufferParameter(buffer, ptr, flags);
                    } else if (ptr.isNil()) {
                        buffer.putAddress(0L);
                    } else if (depth < MAXRECURSE && ptr.respondsTo("to_ptr")) {
                        parameter = ptr;
                        continue;
                    } else {
                        throw context.getRuntime().newArgumentError("to_ptr returned an invalid pointer");
                    }
                    break;
                }
            } else {
                throw context.getRuntime().newArgumentError("Invalid buffer/pointer parameter");
            }
        }
        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            marshal(invocation.getThreadContext(), buffer, parameter);
        }
    }

    /**
     * Converts a ruby String into a native pointer.
     */
    static final class StringMarshaller extends BaseMarshaller {
        
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            if (parameter instanceof RubyString) {
                Util.checkStringSafety(context.getRuntime(), parameter);
                ByteList bl = ((RubyString) parameter).getByteList();
                buffer.putArray(bl.unsafeBytes(), bl.begin(), bl.length(),
                        ArrayFlags.IN | ArrayFlags.NULTERMINATE);
            } else if (parameter.isNil()) {
                buffer.putAddress(0);
            } else {
                throw context.getRuntime().newArgumentError("Invalid string parameter");
            }
        }

        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            marshal(invocation.getThreadContext(), buffer, parameter);
        }
        public static final ParameterMarshaller INSTANCE = new StringMarshaller();
    }

    /**
     * Converts a ruby String into a native pointer.
     */
    static final class StructByValueMarshaller extends BaseMarshaller {
        private final StructLayout layout;
        public StructByValueMarshaller(org.jruby.ext.ffi.StructByValue sbv) {
            layout = sbv.getStructLayout();
        }


        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            if (!(parameter instanceof Struct)) {
                throw context.getRuntime().newTypeError("wrong argument type "
                        + parameter.getMetaClass().getName() + " (expected instance of FFI::Struct)");
            }

            IRubyObject memory = ((Struct) parameter).getMemory();
            if (!(memory instanceof AbstractMemory)) {
                throw context.getRuntime().newTypeError("wrong struct memory type "
                        + memory.getMetaClass().getName());
            }

            if (((AbstractMemory) memory).getSize() < layout.getSize()) {
                throw context.getRuntime().newArgumentError("struct memory too small for parameter");
            }

            MemoryIO io = ((AbstractMemory) memory).getMemoryIO();
            if (io instanceof DirectMemoryIO) {
                if (io.isNull()) {
                    throw context.getRuntime().newRuntimeError("Cannot use a NULL pointer as a struct by value argument");
                }
                buffer.putStruct(((DirectMemoryIO) io).getAddress());
            } else if (io instanceof ArrayMemoryIO) {
                ArrayMemoryIO aio = (ArrayMemoryIO) io;
                buffer.putStruct(aio.array(), aio.arrayOffset());
            } else {
                throw context.getRuntime().newRuntimeError("Invalid struct memory");
            }
        }

        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            marshal(invocation.getThreadContext(), buffer, parameter);
        }
    }
}
