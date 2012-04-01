
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import com.kenai.jffi.InvocationBuffer;
import com.kenai.jffi.Invoker;
import com.kenai.jffi.ArrayFlags;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.Buffer;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.MappedType;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.MemoryPointer;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Struct;
import org.jruby.ext.ffi.StructByValue;
import org.jruby.ext.ffi.StructLayout;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


public final class DefaultMethodFactory extends MethodFactory {

    private static final class SingletonHolder {
        private static final DefaultMethodFactory INSTANCE = new DefaultMethodFactory();
    }
    
    public static DefaultMethodFactory getFactory() {
        return SingletonHolder.INSTANCE;
    }

    private DefaultMethodFactory() {}

    
    @Override
    boolean isSupported(Type returnType, Type[] parameterTypes, CallingConvention convention) {
        return true;
    }
    
    DynamicMethod createMethod(RubyModule module, Function function, 
            Type returnType, Type[] parameterTypes, CallingConvention convention, IRubyObject enums, boolean ignoreError) {

        FunctionInvoker functionInvoker = getFunctionInvoker(returnType);

        ParameterMarshaller[] marshallers = new ParameterMarshaller[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i)  {
            marshallers[i] = getMarshaller(parameterTypes[i], convention, enums);
            if (marshallers[i] == null) {
                throw module.getRuntime().newTypeError("Could not create marshaller for " + parameterTypes[i]);
            }
        }

        Signature signature = new Signature(returnType, parameterTypes, convention, 
                ignoreError, enums instanceof RubyHash ? (RubyHash) enums : null);

        switch (parameterTypes.length) {
            case 0:
                return new DefaultMethodZeroArg(module, function, functionInvoker, signature);
            
            case 1:
                return new DefaultMethodOneArg(module, function, functionInvoker, marshallers, signature);
            
            case 2:
                return new DefaultMethodTwoArg(module, function, functionInvoker, marshallers, signature);
            
            case 3:
                return new DefaultMethodThreeArg(module, function, functionInvoker, marshallers, signature);
            
            case 4:
                return new DefaultMethodFourArg(module, function, functionInvoker, marshallers, signature);
            
            case 5:
                return new DefaultMethodFiveArg(module, function, functionInvoker, marshallers, signature);
            
            case 6:
                return new DefaultMethodSixArg(module, function, functionInvoker, marshallers, signature);
            
            default:
                return new DefaultMethod(module, function, functionInvoker, marshallers, signature);
        }
    }

    static FunctionInvoker getFunctionInvoker(Type returnType) {
        if (returnType instanceof Type.Builtin) {
            return getFunctionInvoker(returnType.getNativeType());

        } else if (returnType instanceof CallbackInfo) {
            return new ConvertingInvoker(getFunctionInvoker(NativeType.POINTER), 
                    DataConverters.getResultConverter(returnType));

        } else if (returnType instanceof StructByValue) {
            return new StructByValueInvoker((StructByValue) returnType);
        
        } else if (returnType instanceof MappedType) {
            MappedType ctype = (MappedType) returnType;
            return new ConvertingInvoker(getFunctionInvoker(ctype.getRealType()), 
                    DataConverters.getResultConverter(ctype));
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
                return Platform.getPlatform().addressSize() == 32 ? Pointer32Invoker.INSTANCE : Pointer64Invoker.INSTANCE;
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
            case TRANSIENT_STRING:
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
    static ParameterMarshaller getMarshaller(Type type, CallingConvention convention, IRubyObject enums) {
        if (type instanceof Type.Builtin) {
            return enums != null && !enums.isNil() ? getEnumMarshaller(type, convention, enums) : getMarshaller(type.getNativeType());

        } else if (type instanceof org.jruby.ext.ffi.CallbackInfo) {
            return new ConvertingMarshaller(getMarshaller(type.getNativeType()), 
                    DataConverters.getParameterConverter(type, null));

        } else if (type instanceof org.jruby.ext.ffi.StructByValue) {
            return new StructByValueMarshaller((org.jruby.ext.ffi.StructByValue) type);
        
        } else if (type instanceof org.jruby.ext.ffi.MappedType) {
            MappedType ctype = (MappedType) type;
            return new ConvertingMarshaller(
                    getMarshaller(ctype.getRealType(), convention, enums), 
                    DataConverters.getParameterConverter(type, 
                        enums instanceof RubyHash ? (RubyHash) enums : null));

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
    static ParameterMarshaller getEnumMarshaller(Type type, CallingConvention convention, IRubyObject enums) {
        if (!(enums instanceof RubyHash)) {
            throw type.getRuntime().newArgumentError("wrong argument type "
                    + enums.getMetaClass().getName() + " (expected Hash)");
        }
        NativeDataConverter converter = DataConverters.getParameterConverter(type, (RubyHash) enums);
        ParameterMarshaller marshaller = getMarshaller(type.getNativeType());
        return converter != null ? new ConvertingMarshaller(marshaller, converter) : marshaller;
    }

    /**
     * Gets a marshaller to convert from a ruby type to a native type.
     *
     * @param type The native type to convert to.
     * @return A new <tt>ParameterMarshaller</tt>
     */
    static ParameterMarshaller getMarshaller(NativeType type) {
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
            case TRANSIENT_STRING:
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
        public static final FunctionInvoker INSTANCE = new VoidInvoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            invoker.invokeInt(function, args);
            return context.getRuntime().getNil();
        }
    }

    /**
     * Invokes the native function with a boolean return value.
     * Returns a Boolean to ruby.
     */
    private static final class BooleanInvoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new BooleanInvoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newBoolean(context, invoker.invokeInt(function, args));
        }
    }

    /**
     * Invokes the native function with n signed 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed8Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Signed8Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newSigned8(context, invoker.invokeInt(function, args));
        }
    }

    /**
     * Invokes the native function with an unsigned 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned8Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Unsigned8Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newUnsigned8(context, invoker.invokeInt(function, args));
        }
    }

    /**
     * Invokes the native function with n signed 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed16Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Signed16Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newSigned16(context, invoker.invokeInt(function, args));
        }
    }

    /**
     * Invokes the native function with an unsigned 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned16Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Unsigned16Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newUnsigned16(context, invoker.invokeInt(function, args));
        }
    }
    /**
     * Invokes the native function with a 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed32Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Signed32Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newSigned32(context, invoker.invokeInt(function, args));
        }
    }

    /**
     * Invokes the native function with an unsigned 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned32Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Unsigned32Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newUnsigned32(context, invoker.invokeInt(function, args));
        }
    }

    /**
     * Invokes the native function with a 64 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed64Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Signed64Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newSigned64(context, invoker.invokeLong(function, args));
        }
    }

    /**
     * Invokes the native function with a 64 bit unsigned integer return value.
     * Returns a ruby Fixnum or Bignum.
     */
    private static final class Unsigned64Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Unsigned64Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newUnsigned64(context, invoker.invokeLong(function, args));
        }
    }

    /**
     * Invokes the native function with a 32 bit float return value.
     * Returns a Float to ruby.
     */
    private static final class Float32Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Float32Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return context.getRuntime().newFloat(invoker.invokeFloat(function, args));
        }
    }

    /**
     * Invokes the native function with a 64 bit float return value.
     * Returns a Float to ruby.
     */
    private static final class Float64Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Float64Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return context.getRuntime().newFloat(invoker.invokeDouble(function, args));
        }
    }

    /**
     * Invokes the native function with a native pointer return value.
     * Returns a {@link MemoryPointer} to ruby.
     */
    private static final class Pointer32Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Pointer32Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newPointer32(context, invoker.invokeAddress(function, args));
        }
    }

    /**
     * Invokes the native function with a native pointer return value.
     * Returns a {@link MemoryPointer} to ruby.
     */
    private static final class Pointer64Invoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new Pointer64Invoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newPointer64(context, invoker.invokeAddress(function, args));
        }
    }

    /**
     * Invokes the native function with a native string return value.
     * Returns a {@link RubyString} to ruby.
     */
    private static final class StringInvoker extends BaseInvoker {
        public static final FunctionInvoker INSTANCE = new StringInvoker();
        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return JITRuntime.newString(context, invoker.invokeAddress(function, args));
        }
    }

    /**
     * Invokes the native function with a native struct return value.
     * Returns a FFI::Struct instance to ruby.
     */
    private static final class StructByValueInvoker extends BaseInvoker {
        private final StructByValue info;

        public StructByValueInvoker(StructByValue info) {
            this.info = info;
        }

        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            int size = info.getStructLayout().getSize();
            Buffer buf = new Buffer(context.getRuntime(), size);
            MemoryIO mem = buf.getMemoryIO();
            byte[] array;
            int arrayOffset;
            if (mem instanceof ArrayMemoryIO) {
                ArrayMemoryIO arrayMemoryIO = (ArrayMemoryIO) mem;
                array = arrayMemoryIO.array();
                arrayOffset = arrayMemoryIO.arrayOffset();
            } else {
                array = new byte[size];
                arrayOffset = 0;
            }

            invoker.invokeStruct(function, args, array, arrayOffset);

            if (!(mem instanceof ArrayMemoryIO)) {
                mem.put(0, array, 0, array.length);
            }

            return info.getStructClass().newInstance(context, buf, Block.NULL_BLOCK);
        }
    }

    /**
     * Invokes the native function, then passes the return value off to a
     * conversion method to massage it to a custom ruby type.
     */
    private static final class ConvertingInvoker extends BaseInvoker {
        private final FunctionInvoker nativeInvoker;
        private final NativeDataConverter converter;

        public ConvertingInvoker(FunctionInvoker nativeInvoker, NativeDataConverter converter) {
            this.nativeInvoker = nativeInvoker;
            this.converter = converter;
        }

        public final IRubyObject invoke(ThreadContext context, Function function, HeapInvocationBuffer args) {
            return converter.fromNative(context, nativeInvoker.invoke(context, function, args));
        }
    }

    /*------------------------------------------------------------------------*/
    static abstract class NonSessionMarshaller implements ParameterMarshaller {
        public final boolean requiresPostInvoke() {
            return false;
        }

        public final boolean requiresReference() {
            return false;
        }

        public final void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            marshal(invocation.getThreadContext(), buffer, parameter);
        }
    }

    /**
     * Converts a ruby Boolean into an 32 bit native integer.
     */
    static final class BooleanMarshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new BooleanMarshaller();

        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putByte(JITRuntime.boolValue32(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into an 8 bit native integer.
     */
    static final class Signed8Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed8Marshaller();

        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putByte(JITRuntime.s8Value32(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into an 8 bit native unsigned integer.
     */
    static final class Unsigned8Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned8Marshaller();

        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putByte(JITRuntime.u8Value32(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into a 16 bit native signed integer.
     */
    static final class Signed16Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed16Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putShort(JITRuntime.s16Value32(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into a 16 bit native unsigned integer.
     */
    static final class Unsigned16Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned16Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putShort(JITRuntime.u16Value32(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into a 32 bit native signed integer.
     */
    static final class Signed32Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed32Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putInt(JITRuntime.s32Value32(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into a 32 bit native unsigned integer.
     */
    static final class Unsigned32Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned32Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putInt(JITRuntime.u32Value32(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into a 64 bit native signed integer.
     */
    static final class Signed64Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed64Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putLong(JITRuntime.s64Value64(parameter));
        }
    }

    /**
     * Converts a ruby Fixnum into a 64 bit native unsigned integer.
     */
    static final class Unsigned64Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned64Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putLong(JITRuntime.u64Value64(parameter));
        }
    }

    /**
     * Converts a ruby Float into a 32 bit native float.
     */
    static final class Float32Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Float32Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putFloat((float) RubyNumeric.num2dbl(parameter));
        }
    }

    /**
     * Converts a ruby Float into a 64 bit native float.
     */
    static final class Float64Marshaller extends NonSessionMarshaller {
        public static final ParameterMarshaller INSTANCE = new Float64Marshaller();
        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            buffer.putDouble(RubyNumeric.num2dbl(parameter));
        }
    }

    static abstract class PointerParameterMarshaller extends NonSessionMarshaller {
        private final int flags;
        public PointerParameterMarshaller(int flags) {
            this.flags = flags;
        }

        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter,
                                  PointerParameterStrategy strategy) {
            if (strategy.isDirect()) {
                buffer.putAddress(strategy.address(parameter));

            } else {
                buffer.putArray(byte[].class.cast(strategy.object(parameter)), strategy.offset(parameter), strategy.length(parameter),
                        flags);
            }
        }
    }
    /**
     * Converts a ruby Buffer into a native address.
     */
    static final class BufferMarshaller extends PointerParameterMarshaller {
        static final ParameterMarshaller IN = new BufferMarshaller(ArrayFlags.IN);
        static final ParameterMarshaller OUT = new BufferMarshaller(ArrayFlags.OUT);
        static final ParameterMarshaller INOUT = new BufferMarshaller(ArrayFlags.IN | ArrayFlags.OUT);

        public BufferMarshaller(int flags) {
            super(flags);
        }

        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            marshal(context, buffer, parameter, JITRuntime.pointerParameterStrategy(parameter));
        }
    }

    /**
     * Converts a ruby String into a native pointer.
     */
    static final class StringMarshaller extends PointerParameterMarshaller {
        public static final ParameterMarshaller INSTANCE = new StringMarshaller(ArrayFlags.IN | ArrayFlags.NULTERMINATE);

        StringMarshaller(int flags) {
            super(flags);
        }

        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            marshal(context, buffer, parameter, JITRuntime.stringParameterStrategy(parameter));
        }
    }

    /**
     * Converts a ruby String into a native pointer.
     */
    static final class StructByValueMarshaller extends NonSessionMarshaller {
        private final StructLayout layout;
        public StructByValueMarshaller(org.jruby.ext.ffi.StructByValue sbv) {
            layout = sbv.getStructLayout();
        }


        public final void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            if (!(parameter instanceof Struct)) {
                throw context.getRuntime().newTypeError("wrong argument type "
                        + parameter.getMetaClass().getName() + " (expected instance of FFI::Struct)");
            }

            final AbstractMemory memory = ((Struct) parameter).getMemory();
            if (memory.getSize() < layout.getSize()) {
                throw context.getRuntime().newArgumentError("struct memory too small for parameter");
            }

            final MemoryIO io = memory.getMemoryIO();
            if (io instanceof DirectMemoryIO) {
                if (io.isNull()) {
                    throw context.getRuntime().newRuntimeError("Cannot use a NULL pointer as a struct by value argument");
                }
                buffer.putStruct(((DirectMemoryIO) io).getAddress());

            } else if (io instanceof ArrayMemoryIO) {
                ArrayMemoryIO aio = (ArrayMemoryIO) io;
                buffer.putStruct(aio.array(), aio.arrayOffset());

            } else {
                throw context.getRuntime().newRuntimeError("invalid struct memory");
            }
        }
    }
    
    static final class ConvertingMarshaller implements ParameterMarshaller {
        private final ParameterMarshaller nativeMarshaller;
        private final NativeDataConverter converter;

        public ConvertingMarshaller(ParameterMarshaller nativeMarshaller, NativeDataConverter converter) {
            this.nativeMarshaller = nativeMarshaller;
            this.converter = converter;
        }


        public void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject parameter) {
            ThreadContext context = invocation.getThreadContext();
            final IRubyObject nativeValue = converter.toNative(context, parameter);

            // keep a hard ref to the converted value if needed
            if (converter.isReferenceRequired()) {
                invocation.addReference(nativeValue);
            }
            nativeMarshaller.marshal(context, buffer, nativeValue);
        }

        public void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject parameter) {
            nativeMarshaller.marshal(context, buffer, converter.toNative(context, parameter));
        }

        public boolean requiresPostInvoke() {
            return converter.isReferenceRequired();
        }

        public boolean requiresReference() {
            return converter.isReferenceRequired();
        }
    }
}
