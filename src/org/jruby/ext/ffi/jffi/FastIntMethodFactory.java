
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyBoolean;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.MappedType;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Struct;
import org.jruby.ext.ffi.Type;
import org.jruby.ext.ffi.Util;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class FastIntMethodFactory extends MethodFactory {
    private static final class SingletonHolder {
        private static final FastIntMethodFactory INSTANCE = new FastIntMethodFactory();
    }
    private FastIntMethodFactory() {}
    public static final FastIntMethodFactory getFactory() {
        return SingletonHolder.INSTANCE;
    }
    
    final boolean isFastIntMethod(Type returnType, Type[] parameterTypes) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (!isFastIntParam(parameterTypes[i])) {
                return false;
            }
        }

        return parameterTypes.length <= 3 && isFastIntResult(returnType)
                && (Platform.getPlatform().getCPU() == Platform.CPU.I386
                    || Platform.getPlatform().getCPU() == Platform.CPU.X86_64);
    }

    
    final boolean isFastIntResult(Type type) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case VOID:
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case BOOL:
                    return true;
                case POINTER:
                case STRING:
                    return Platform.getPlatform().addressSize() == 32;
                case LONG:
                case ULONG:
                    return Platform.getPlatform().longSize() == 32;
            }

        } else if (type instanceof MappedType) {
            return isFastIntResult(((MappedType) type).getRealType());

        }
        return false;
    }
    
    final boolean isFastIntParam(Type paramType) {
        if (paramType instanceof Type.Builtin) {
            switch (paramType.getNativeType()) {
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case BOOL:
//                case FLOAT:
                    return true;
                case LONG:
                case ULONG:
                    return Platform.getPlatform().longSize() == 32;
            }

        } else if (paramType instanceof MappedType) {
            MappedType mt = (MappedType) paramType;
            return isFastIntParam(mt.getRealType()) && !mt.isReferenceRequired() && !mt.isPostInvokeRequired();

        }

        return false;
    }
    
    DynamicMethod createMethod(RubyModule module, Function function, 
            Type returnType, Type[] parameterTypes, IRubyObject enums) {

        IntParameterConverter[] parameterConverters = new IntParameterConverter[parameterTypes.length];
        IntResultConverter resultConverter = getIntResultConverter(returnType);

        for (int i = 0; i < parameterConverters.length; ++i) {
            parameterConverters[i] = getIntParameterConverter(parameterTypes[i], enums);
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
    
    final IntParameterConverter getIntParameterConverter(Type type, IRubyObject enums) {
        if (type instanceof Type.Builtin) {
            return getIntParameterConverter(type.getNativeType(), enums);

        } else if (type instanceof MappedType) {
            MappedType ctype = (MappedType) type;
            return new MappedParameterConverter(getIntParameterConverter(ctype.getRealType(), enums), ctype);

        } else {
            return null;
        }
    }
    
    final IntParameterConverter getIntParameterConverter(NativeParam type, IRubyObject enums) {
        switch ((NativeType) type) {
            case BOOL: return BooleanParameterConverter.INSTANCE;
            case CHAR: return Signed8ParameterConverter.INSTANCE;
            case UCHAR: return Unsigned8ParameterConverter.INSTANCE;
            case SHORT: return Signed16ParameterConverter.INSTANCE;
            case USHORT: return Unsigned16ParameterConverter.INSTANCE;
            case INT: return enums instanceof RubyHash 
                    ? new IntOrEnumParameterConverter((RubyHash) enums)
                    : Signed32ParameterConverter.INSTANCE;
            case UINT: return Unsigned32ParameterConverter.INSTANCE;
            case FLOAT: return Float32ParameterConverter.INSTANCE;
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
            case POINTER:
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
                if (Platform.getPlatform().addressSize() == 32) {
                    return PointerParameterConverter.INSTANCE;
                }
                throw new IllegalArgumentException("Pointer is too big for int parameter");
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }

    final IntResultConverter getIntResultConverter(Type type) {
        if (type instanceof Type.Builtin) {
            return getIntResultConverter(type.getNativeType());

        } else if (type instanceof MappedType) {
            MappedType ctype = (MappedType) type;
            return new MappedResultConverter(getIntResultConverter(ctype.getRealType()), ctype);

        } else {
            return null;
        }
    }

    final IntResultConverter getIntResultConverter(NativeType type) {
        switch (type) {
            case VOID: return VoidResultConverter.INSTANCE;
            case BOOL: return BooleanResultConverter.INSTANCE;
            case CHAR: return Signed8ResultConverter.INSTANCE;
            case UCHAR: return Unsigned8ResultConverter.INSTANCE;
            case SHORT: return Signed16ResultConverter.INSTANCE;
            case USHORT: return Unsigned16ResultConverter.INSTANCE;
            case INT: return Signed32ResultConverter.INSTANCE;
            case UINT: return Unsigned32ResultConverter.INSTANCE;
            case FLOAT: return Float32ResultConverter.INSTANCE;
            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Signed32ResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException(":long is too big for int result");
            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Unsigned32ResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException(":ulong is too big for int result");
            case POINTER:
                if (Platform.getPlatform().addressSize() == 32) {
                    return PointerResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException(":pointer is too big for int result");
            case STRING:
                if (Platform.getPlatform().addressSize() == 32) {
                    return StringResultConverter.INSTANCE;
                }
                throw new IllegalArgumentException(":string is too big for int result");
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
    static final class BooleanResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new BooleanResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return context.getRuntime().newBoolean((value & 0xff) != 0);
        }
    }
    static final class Signed8ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Signed8ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newSigned8(context.getRuntime(), (byte) value);
        }
    }
    static final class Unsigned8ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned8ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newUnsigned8(context.getRuntime(), (byte) value);
        }
    }
    static final class Signed16ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Signed16ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newSigned16(context.getRuntime(), (short) value);
        }
    }
    static final class Unsigned16ResultConverter implements IntResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned16ResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            return Util.newUnsigned16(context.getRuntime(), (short) value);
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
            return new Pointer(context.getRuntime(), NativeMemoryIO.wrap(context.getRuntime(), address));
        }
    }

    static final class MappedResultConverter implements IntResultConverter {
        private final IntResultConverter nativeConverter;
        private final MappedType mappedType;

        public MappedResultConverter(IntResultConverter nativeConverter, MappedType mappedType) {
            this.nativeConverter = nativeConverter;
            this.mappedType = mappedType;
        }

        public final IRubyObject fromNative(ThreadContext context, int value) {
            return mappedType.fromNative(context, nativeConverter.fromNative(context, value));
        }
    }


    static final class StringResultConverter implements IntResultConverter {
        private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
        public static final IntResultConverter INSTANCE = new StringResultConverter();
        public final IRubyObject fromNative(ThreadContext context, int value) {
            long address = ((long) value) & PointerResultConverter.ADDRESS_MASK;
            return FFIUtil.getString(context.getRuntime(), address);
        }
    }

    static abstract class BaseParameterConverter implements IntParameterConverter {
        static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();

        public boolean isConvertible(ThreadContext context, IRubyObject value) {
            return true;
        }

    }
    static final class BooleanParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new BooleanParameterConverter();
        public final int intValue(ThreadContext context, IRubyObject obj) {
            if (!(obj instanceof RubyBoolean)) {
                throw context.getRuntime().newTypeError("wrong argument type.  Expected true or false");
            }
            return obj.isTrue() ? 1 : 0;
        }
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
    private static final int getAddress(Pointer ptr) {
        return (int) ((DirectMemoryIO) ptr.getMemoryIO()).getAddress();
    }
    static final class PointerParameterConverter extends BaseParameterConverter {

        public static final IntParameterConverter INSTANCE = new PointerParameterConverter();

        public final int intValue(ThreadContext context, IRubyObject parameter) {

            if (parameter instanceof Pointer) {
                return getAddress((Pointer) parameter);

            } else if (parameter instanceof Struct) {
                IRubyObject memory = ((Struct) parameter).getMemory();
                if (memory instanceof Pointer) {
                    return getAddress((Pointer) memory);
                } else if (memory == null || memory.isNil()) {
                    return 0;
                }

            } else if (parameter.isNil()) {
                return 0;
            }
            throw context.getRuntime().newArgumentError("Cannot convert pointer to 32bit integer");
        }

        @Override
        public boolean isConvertible(ThreadContext context, IRubyObject parameter) {
            return parameter instanceof Pointer
                    || (parameter instanceof Struct && ((Struct) parameter).getMemory() instanceof Pointer)
                    || parameter.isNil();
        }


    }

    static final class IntOrEnumParameterConverter extends BaseParameterConverter {
        private final RubyHash enums;

        public IntOrEnumParameterConverter(RubyHash enums) {
            this.enums = enums;
        }

        public final int intValue(ThreadContext context, IRubyObject parameter) {
            return Util.intValue(parameter, enums);
        }
    }
    
    static final class MappedParameterConverter extends BaseParameterConverter {
        private final IntParameterConverter nativeConverter;
        private final MappedType mappedType;

        public MappedParameterConverter(IntParameterConverter nativeConverter, MappedType mappedType) {
            this.nativeConverter = nativeConverter;
            this.mappedType = mappedType;
        }

        public final int intValue(ThreadContext context, IRubyObject obj) {
            return nativeConverter.intValue(context, mappedType.toNative(context, obj));
        }

        @Override
        public boolean isConvertible(ThreadContext context, IRubyObject value) {
            return nativeConverter.isConvertible(context, mappedType.toNative(context, value));
        }

    }
}
