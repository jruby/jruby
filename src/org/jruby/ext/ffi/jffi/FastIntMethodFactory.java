
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.ext.ffi.BasePointer;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Util;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class FastIntMethodFactory {
    private static final class SingletonHolder {
        private static final FastIntMethodFactory INSTANCE = new FastIntMethodFactory();
    }
    private FastIntMethodFactory() {}
    public static final FastIntMethodFactory getFactory() {
        return SingletonHolder.INSTANCE;
    }
    final boolean isFastIntMethod(NativeType returnType, NativeParam[] parameterTypes) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (!isFastIntParam(parameterTypes[i])) {
                return false;
            }
        }
        return parameterTypes.length <= 3 && isFastIntResult(returnType);
    }
    final boolean isFastIntResult(NativeType type) {
        switch (type) {
            case VOID:
            case INT8:
            case UINT8:
            case INT16:
            case UINT16:
            case INT32:
            case UINT32:
                return true;
            case POINTER:
            case STRING:
                return Platform.getPlatform().addressSize() == 32;
            case LONG:
            case ULONG:
                return Platform.getPlatform().longSize() == 32;
            default:
                return false;
        }
    }
    final boolean isFastIntParam(NativeParam paramType) {
        if (paramType instanceof NativeType) {
            switch ((NativeType) paramType) {
                case INT8:
                case UINT8:
                case INT16:
                case UINT16:
                case INT32:
                case UINT32:
//                case FLOAT32:
                    return true;
                case LONG:
                case ULONG:
                    return Platform.getPlatform().longSize() == 32;
            }
        }
        return false;
    }
    DynamicMethod createMethod(RubyModule module, Function function,
            NativeType returnType, NativeParam[] parameterTypes) {
        FastIntMethodFactory factory = this;
        IntParameterConverter[] parameterConverters = new IntParameterConverter[parameterTypes.length];
        IntResultConverter resultConverter = factory.getIntResultConverter(returnType);
        for (int i = 0; i < parameterConverters.length; ++i) {
            parameterConverters[i] = factory.getIntParameterConverter(parameterTypes[i]);
        }
        switch (parameterTypes.length) {
            case 0:
                return new FastIntMethodZeroArg(module, function, resultConverter, parameterConverters);
            case 1:
                return new FastIntMethodOneArg(module, function, resultConverter, parameterConverters);
            case 2:
                return new FastIntMethodTwoArg(module, function, resultConverter, parameterConverters);
            case 3:
                return new FastIntMethodThreeArg(module, function, resultConverter, parameterConverters);
            default:
                throw module.getRuntime().newRuntimeError("Arity " + parameterTypes.length + " not implemented");
        }
    }
    final IntParameterConverter getIntParameterConverter(NativeParam type) {
        switch ((NativeType) type) {
            case INT8: return Signed8ParameterConverter.INSTANCE;
            case UINT8: return Unsigned8ParameterConverter.INSTANCE;
            case INT16: return Signed16ParameterConverter.INSTANCE;
            case UINT16: return Unsigned16ParameterConverter.INSTANCE;
            case INT32: return Signed32ParameterConverter.INSTANCE;
            case UINT32: return Unsigned32ParameterConverter.INSTANCE;
            case FLOAT32: return Float32ParameterConverter.INSTANCE;
            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Signed32ParameterConverter.INSTANCE;
                }
                throw new IllegalArgumentException("Long is too big for int parameter");
            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Unsigned32ParameterConverter.INSTANCE;
                }
                throw new IllegalArgumentException("Long is too big for int parameter");
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }
    final IntResultConverter getIntResultConverter(NativeType type) {
        switch (type) {
            case VOID: return VoidResultConverter.INSTANCE;
            case INT8: return Signed8ResultConverter.INSTANCE;
            case UINT8: return Unsigned8ResultConverter.INSTANCE;
            case INT16: return Signed16ResultConverter.INSTANCE;
            case UINT16: return Unsigned16ResultConverter.INSTANCE;
            case INT32: return Signed32ResultConverter.INSTANCE;
            case UINT32: return Unsigned32ResultConverter.INSTANCE;
            case FLOAT32: return Float32ResultConverter.INSTANCE;
            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Signed32ResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException("Long is too big for int parameter");
            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Unsigned32ResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException("Long is too big for int parameter");
            case POINTER:
                if (Platform.getPlatform().addressSize() == 32) {
                    return PointerResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException("Pointer is too big for int parameter");
            case STRING:
                if (Platform.getPlatform().addressSize() == 32) {
                    return StringResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException("Long is too big for int parameter");
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }
    static final class VoidResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new VoidResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return context.getRuntime().getNil();
        }
    }
    static final class Signed8ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Signed8ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newSigned8(context.getRuntime(), value);
        }
    }
    static final class Unsigned8ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned8ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newUnsigned8(context.getRuntime(), value);
        }
    }
    static final class Signed16ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Signed16ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newSigned16(context.getRuntime(), value);
        }
    }
    static final class Unsigned16ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned16ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newUnsigned16(context.getRuntime(), value);
        }
    }
    static final class Signed32ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Signed32ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newSigned32(context.getRuntime(), value);
        }
    }
    static final class Unsigned32ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned32ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newUnsigned32(context.getRuntime(), value);
        }
    }
    static final class Float32ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Float32ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return context.getRuntime().newFloat(Float.intBitsToFloat(value));
        }
    }
    static final class PointerResultConverter implements IntResultConverter {
        static final long ADDRESS_MASK = Platform.getPlatform().addressSize() == 32
                ? 0xffffffffL : 0xffffffffffffffffL;
        public static final IntResultConverter INSTANCE = new PointerResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            final long address = ((long) value) & ADDRESS_MASK;
            return new BasePointer(context.getRuntime(),
                    address != 0 ? new NativeMemoryIO(address) : new NullMemoryIO(context.getRuntime()));
        }
    }

    static final class StringResultConverter implements IntResultConverter {
        private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
        public static final IntResultConverter INSTANCE = new StringResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            long address = ((long) value) & PointerResultConverter.ADDRESS_MASK;
            if (address == 0) {
                return context.getRuntime().getNil();
            }
            int len = (int) IO.getStringLength(address);
            if (len == 0) {
                return RubyString.newEmptyString(context.getRuntime());
            }
            byte[] bytes = new byte[len];
            IO.getByteArray(address, bytes, 0, len);

            RubyString s =  RubyString.newStringShared(context.getRuntime(), bytes);
            s.setTaint(true);
            return s;
        }
    }
    static abstract class BaseParameterConverter implements IntParameterConverter {
        static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    }
    static final class Signed8ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Signed8ParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            return Util.int8Value(obj);
        }
    }
    static final class Unsigned8ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Unsigned8ParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            return Util.uint8Value(obj);
        }
    }
    static final class Signed16ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Signed16ParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            return Util.int16Value(obj);
        }
    }
    static final class Unsigned16ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Unsigned16ParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            return Util.uint16Value(obj);
        }
    }
    static final class Signed32ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Signed32ParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            return Util.int32Value(obj);
        }
    }
    static final class Unsigned32ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Unsigned32ParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            return (int) Util.uint32Value(obj);
        }
    }
    static final class Float32ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Float32ParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            return Float.floatToRawIntBits((float) RubyNumeric.num2dbl(obj));
        }
    }
}
