
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyBoolean;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Type;
import org.jruby.ext.ffi.Util;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class FastLongMethodFactory {
    private static final class SingletonHolder {
        private static final FastLongMethodFactory INSTANCE = new FastLongMethodFactory();
    }
    private FastLongMethodFactory() {}
    public static final FastLongMethodFactory getFactory() {
        return SingletonHolder.INSTANCE;
    }
    
    final boolean isFastLongMethod(Type returnType, Type[] parameterTypes) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (!isFastLongParam(parameterTypes[i])) {
                return false;
            }
        }
        return parameterTypes.length <= 3 && isFastLongResult(returnType);
    }
    
    
    final boolean isFastLongResult(Type type) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case VOID:
                case BOOL:
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG_LONG:
                case ULONG_LONG:
                case POINTER:
                case STRING:
                case LONG:
                case ULONG:
                    return true;
            }
        }
        return false;
    }
    
    final boolean isFastLongParam(Type paramType) {
        if (paramType instanceof Type.Builtin) {
            switch (paramType.getNativeType()) {
                case BOOL:
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG_LONG:
                case ULONG_LONG:
                case LONG:
                case ULONG:
                    return true;
            }
        }
        return false;
    }

    DynamicMethod createMethod(RubyModule module, Function function,
            Type returnType, Type[] parameterTypes) {

        LongParameterConverter[] parameterConverters = new LongParameterConverter[parameterTypes.length];
        LongResultConverter resultConverter = getLongResultConverter(returnType);
        for (int i = 0; i < parameterConverters.length; ++i) {
            parameterConverters[i] = getLongParameterConverter(parameterTypes[i]);
        }

        switch (parameterTypes.length) {
            case 0:
                return new FastLongMethodZeroArg(module, function, resultConverter, parameterConverters);
            case 1:
                return new FastLongMethodOneArg(module, function, resultConverter, parameterConverters);
            case 2:
                return new FastLongMethodTwoArg(module, function, resultConverter, parameterConverters);
            case 3:
                return new FastLongMethodThreeArg(module, function, resultConverter, parameterConverters);
            default:
                throw module.getRuntime().newRuntimeError("Arity " + parameterTypes.length + " not implemented");
        }
    }
    final LongParameterConverter getLongParameterConverter(Type type) {
        switch (type.getNativeType()) {
            case BOOL: return BooleanParameterConverter.INSTANCE;
            case CHAR: return Signed8ParameterConverter.INSTANCE;
            case UCHAR: return Unsigned8ParameterConverter.INSTANCE;
            case SHORT: return Signed16ParameterConverter.INSTANCE;
            case USHORT: return Unsigned16ParameterConverter.INSTANCE;
            case INT: return Signed32ParameterConverter.INSTANCE;
            case UINT: return Unsigned32ParameterConverter.INSTANCE;
            case LONG_LONG: return Signed64ParameterConverter.INSTANCE;
            case ULONG_LONG: return Unsigned64ParameterConverter.INSTANCE;
            case FLOAT: return Float32ParameterConverter.INSTANCE;
            case DOUBLE: return Float64ParameterConverter.INSTANCE;
            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Signed32ParameterConverter.INSTANCE;
                } else {
                    return Signed64ParameterConverter.INSTANCE;
                }
            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Unsigned32ParameterConverter.INSTANCE;
                } else {
                    return Unsigned64ParameterConverter.INSTANCE;
                }
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }
    final LongResultConverter getLongResultConverter(Type type) {
        switch (type.getNativeType()) {
            case VOID: return VoidResultConverter.INSTANCE;
            case BOOL: return BooleanResultConverter.INSTANCE;
            case CHAR: return Signed8ResultConverter.INSTANCE;
            case UCHAR: return Unsigned8ResultConverter.INSTANCE;
            case SHORT: return Signed16ResultConverter.INSTANCE;
            case USHORT: return Unsigned16ResultConverter.INSTANCE;
            case INT: return Signed32ResultConverter.INSTANCE;
            case UINT: return Unsigned32ResultConverter.INSTANCE;
            case LONG_LONG: return Signed64ResultConverter.INSTANCE;
            case ULONG_LONG: return Unsigned64ResultConverter.INSTANCE;
            case FLOAT: return Float32ResultConverter.INSTANCE;
            case DOUBLE: return Float64ResultConverter.INSTANCE;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                    ? Signed32ResultConverter.INSTANCE
                    : Signed64ResultConverter.INSTANCE;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                    ? Unsigned32ResultConverter.INSTANCE
                    : Unsigned64ResultConverter.INSTANCE;
            case POINTER:
                return PointerResultConverter.INSTANCE;
            case STRING:
                return StringResultConverter.INSTANCE;
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }
    static final class VoidResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new VoidResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return context.getRuntime().getNil();
        }
    }

    static final class BooleanResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Signed8ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return context.getRuntime().newBoolean(value != 0);
        }
    }

    static final class Signed8ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Signed8ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newSigned8(context.getRuntime(), (byte) value);
        }
    }
    static final class Unsigned8ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Unsigned8ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newUnsigned8(context.getRuntime(), (byte) value);
        }
    }
    static final class Signed16ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Signed16ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newSigned16(context.getRuntime(), (short) value);
        }
    }
    static final class Unsigned16ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Unsigned16ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newUnsigned16(context.getRuntime(), (short) value);
        }
    }
    static final class Signed32ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Signed32ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newSigned32(context.getRuntime(), (int) value);
        }
    }
    static final class Unsigned32ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Unsigned32ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newUnsigned32(context.getRuntime(), (int) value);
        }
    }
    static final class Signed64ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Signed64ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newSigned64(context.getRuntime(), value);
        }
    }
    static final class Unsigned64ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Unsigned64ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return Util.newUnsigned64(context.getRuntime(), value);
        }
    }
    static final class Float32ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Float32ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return context.getRuntime().newFloat(Float.intBitsToFloat((int) value));
        }
    }
    static final class Float64ResultConverter implements LongResultConverter {
        public static final LongResultConverter INSTANCE = new Float64ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            return context.getRuntime().newFloat(Double.longBitsToDouble(value));
        }
    }

    static final class PointerResultConverter implements LongResultConverter {
        static final long ADDRESS_MASK = Platform.getPlatform().addressSize() == 32
                ? 0xffffffffL : 0xffffffffffffffffL;
        public static final LongResultConverter INSTANCE = new PointerResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            final long address = ((long) value) & ADDRESS_MASK;
            return new Pointer(context.getRuntime(),
                    NativeMemoryIO.wrap(context.getRuntime(), address));
        }
    }

    static final class StringResultConverter implements LongResultConverter {
        private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
        public static final LongResultConverter INSTANCE = new StringResultConverter();
        public final IRubyObject fromNative(ThreadContext context, long value) {
            long address = value & PointerResultConverter.ADDRESS_MASK;
            return FFIUtil.getString(context.getRuntime(), address);
        }
    }
    static abstract class BaseParameterConverter implements LongParameterConverter {
        static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    }
    static final class BooleanParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new BooleanParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            if (!(obj instanceof RubyBoolean)) {
                throw context.getRuntime().newTypeError("wrong argument type.  Expected true or false");
            }
            return obj.isTrue() ? 1 : 0;
        }
    }
    static final class Signed8ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Signed8ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.int8Value(obj);
        }
    }
    static final class Unsigned8ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Unsigned8ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.uint8Value(obj);
        }
    }
    static final class Signed16ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Signed16ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.int16Value(obj);
        }
    }
    static final class Unsigned16ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Unsigned16ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.uint16Value(obj);
        }
    }
    static final class Signed32ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Signed32ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.int32Value(obj);
        }
    }
    static final class Unsigned32ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Unsigned32ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.uint32Value(obj);
        }
    }
    static final class Signed64ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Signed64ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.int64Value(obj);
        }
    }
    static final class Unsigned64ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Unsigned64ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Util.uint64Value(obj);
        }
    }
    static final class Float32ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Float32ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Float.floatToRawIntBits((float) RubyNumeric.num2dbl(obj)) & 0xffffffffL;
        }
    }
    static final class Float64ParameterConverter extends BaseParameterConverter {
        public static final LongParameterConverter INSTANCE = new Float64ParameterConverter();
        public final long longValue(ThreadContext context, IRubyObject obj) {
            return Double.doubleToRawLongBits(RubyNumeric.num2dbl(obj));
        }
    }
}
