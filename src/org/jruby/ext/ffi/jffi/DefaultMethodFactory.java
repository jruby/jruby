
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import com.kenai.jffi.InvocationBuffer;
import com.kenai.jffi.Invoker;
import com.kenai.jffi.ObjectBuffer;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.BasePointer;
import org.jruby.ext.ffi.Buffer;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.MemoryPointer;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Struct;
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
            NativeType returnType, NativeParam[] parameterTypes, CallingConvention convention) {
        FunctionInvoker functionInvoker = getFunctionInvoker(returnType);
        ParameterMarshaller[] marshallers = new ParameterMarshaller[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i)  {
            marshallers[i] = getMarshaller(parameterTypes[i], convention);
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
        switch (parameterTypes.length) {
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
    }
    static FunctionInvoker getFunctionInvoker(NativeType returnType) {
        switch (returnType) {
            case VOID:
                return VoidInvoker.INSTANCE;
            case POINTER:
                return PointerInvoker.INSTANCE;
            case INT8:
                return Signed8Invoker.INSTANCE;
            case INT16:
                return Signed16Invoker.INSTANCE;
            case INT32:
                return Signed32Invoker.INSTANCE;
            case UINT8:
                return Unsigned8Invoker.INSTANCE;
            case UINT16:
                return Unsigned16Invoker.INSTANCE;
            case UINT32:
                return Unsigned32Invoker.INSTANCE;
            case INT64:
                return Signed64Invoker.INSTANCE;
            case UINT64:
                return Unsigned64Invoker.INSTANCE;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Invoker.INSTANCE
                        : Signed64Invoker.INSTANCE;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? Unsigned32Invoker.INSTANCE
                        : Unsigned64Invoker.INSTANCE;
            case FLOAT32:
                return Float32Invoker.INSTANCE;
            case FLOAT64:
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
    static final ParameterMarshaller getMarshaller(NativeParam type, CallingConvention convention) {
        if (type instanceof NativeType) {
            return getMarshaller((NativeType) type);
        } else if (type instanceof org.jruby.ext.ffi.CallbackInfo) {
            return new CallbackMarshaller((org.jruby.ext.ffi.CallbackInfo) type, convention);
        } else {
            return null;
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
            case INT8:
                return Signed8Marshaller.INSTANCE;
            case UINT8:
                return Unsigned8Marshaller.INSTANCE;
            case INT16:
                return Signed16Marshaller.INSTANCE;
            case UINT16:
                return Unsigned16Marshaller.INSTANCE;
            case INT32:
                return Signed32Marshaller.INSTANCE;
            case UINT32:
                return Unsigned32Marshaller.INSTANCE;
            case INT64:
                return Signed64Marshaller.INSTANCE;
            case UINT64:
                return Unsigned64Marshaller.INSTANCE;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Marshaller.INSTANCE
                        : Signed64Marshaller.INSTANCE;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Marshaller.INSTANCE
                        : Unsigned64Marshaller.INSTANCE;
            case FLOAT32:
                return Float32Marshaller.INSTANCE;
            case FLOAT64:
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
            return new BasePointer(runtime, address != 0 ? new NativeMemoryIO(address) : new NullMemoryIO(runtime));
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
            long address = invoker.invokeAddress(function, args);
            if (address == 0) {
                return runtime.getNil();
            }
            int len = (int) IO.getStringLength(address);
            if (len == 0) {
                return RubyString.newEmptyString(runtime);
            }
            byte[] bytes = new byte[len];
            IO.getByteArray(address, bytes, 0, len);
            
            RubyString s =  RubyString.newStringShared(runtime, bytes);
            s.setTaint(true);
            return s;
        }
        public static final FunctionInvoker INSTANCE = new StringInvoker();
    }

    /*------------------------------------------------------------------------*/
    static abstract class BaseMarshaller implements ParameterMarshaller {
        public boolean needsInvocationSession() {
            return false;
        }
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
        static final ParameterMarshaller IN = new BufferMarshaller(ObjectBuffer.IN);
        static final ParameterMarshaller OUT = new BufferMarshaller(ObjectBuffer.OUT);
        static final ParameterMarshaller INOUT = new BufferMarshaller(ObjectBuffer.IN | ObjectBuffer.OUT);
        private final int flags;
        public BufferMarshaller(int flags) {
            this.flags = flags;
        }
        private static final int bufferFlags(Buffer buffer) {
            int f = buffer.getInOutFlags();
            return ((f & Buffer.IN) != 0 ? ObjectBuffer.IN: 0)
                    | ((f & Buffer.OUT) != 0 ? ObjectBuffer.OUT : 0);
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
                buffer.putArray(bl.unsafeBytes(), bl.begin(), bl.length(), flags | ObjectBuffer.ZERO_TERMINATE);
            } else if (parameter.respondsTo("to_ptr")) {
                IRubyObject ptr = parameter.callMethod(context, "to_ptr");
                if (ptr instanceof Pointer) {
                    buffer.putAddress(getAddress((Pointer) ptr));
                } else if (ptr instanceof Buffer) {
                    addBufferParameter(buffer, ptr, flags);
                } else if (ptr.isNil()) {
                    buffer.putAddress(0L);
                } else {
                    throw context.getRuntime().newArgumentError("to_ptr returned an invalid pointer");
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
                        ObjectBuffer.IN | ObjectBuffer.ZERO_TERMINATE);
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
}
