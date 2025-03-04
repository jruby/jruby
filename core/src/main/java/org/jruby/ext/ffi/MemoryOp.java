
package org.jruby.ext.ffi;

import java.nio.ByteOrder;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Error.typeError;

/**
 * Defines memory operations for a primitive type
 */
abstract class MemoryOp {
    public static final MemoryOp BOOL = new BooleanOp();
    public static final MemoryOp INT8 = new Signed8();
    public static final MemoryOp UINT8 = new Unsigned8();
    public static final MemoryOp INT16 = new Signed16();
    public static final MemoryOp UINT16 = new Unsigned16();
    public static final MemoryOp INT32 = new Signed32();
    public static final MemoryOp UINT32 = new Unsigned32();
    public static final MemoryOp INT64 = new Signed64();
    public static final MemoryOp UINT64 = new Unsigned64();
    public static final MemoryOp FLOAT32 = new Float32();
    public static final MemoryOp FLOAT64 = new Float64();
    public static final MemoryOp FLOAT128 = new Float128();
    public static final MemoryOp INT16SWAP = new Signed16Swapped();
    public static final MemoryOp UINT16SWAP = new Unsigned16Swapped();
    public static final MemoryOp INT32SWAP = new Signed32Swapped();
    public static final MemoryOp UINT32SWAP = new Unsigned32Swapped();
    public static final MemoryOp INT64SWAP = new Signed64Swapped();
    public static final MemoryOp UINT64SWAP = new Unsigned64Swapped();
    public static final MemoryOp POINTER = new PointerOp();

    public static MemoryOp getMemoryOp(NativeType type) {
        return getMemoryOp(type, ByteOrder.nativeOrder());
    }
    
    public static MemoryOp getMemoryOp(NativeType type, ByteOrder order) {
        switch (type) {
            case BOOL:
                return BOOL;
            case CHAR:
                return INT8;
            case UCHAR:
                return UINT8;
            case SHORT:
                return order.equals(ByteOrder.nativeOrder()) ? INT16 : INT16SWAP;
            case USHORT:
                return order.equals(ByteOrder.nativeOrder()) ? UINT16 : UINT16SWAP;
            case INT:
                return order.equals(ByteOrder.nativeOrder()) ? INT32 : INT32SWAP;
            case UINT:
                return order.equals(ByteOrder.nativeOrder()) ? UINT32 : UINT32SWAP;
            case LONG_LONG:
                return order.equals(ByteOrder.nativeOrder()) ? INT64 : INT64SWAP;
            case ULONG_LONG:
                return order.equals(ByteOrder.nativeOrder()) ? UINT64 : UINT64SWAP;
            case FLOAT:
                return FLOAT32;
            case DOUBLE:
                return FLOAT64;
//            case LONGDOUBLE:
//                return FLOAT128;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? getMemoryOp(NativeType.INT, order) : getMemoryOp(NativeType.LONG_LONG, order);
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? getMemoryOp(NativeType.UINT, order) : getMemoryOp(NativeType.ULONG_LONG, order);
            case POINTER:
                return POINTER;
            default:
                return null;
        }
    }

    public static MemoryOp getMemoryOp(Type type) {
        return getMemoryOp(type, ByteOrder.nativeOrder());
    }

    public static MemoryOp getMemoryOp(Type type, ByteOrder order) {
        if (type instanceof Type.Builtin) {
            return getMemoryOp(type.getNativeType(), order);

        } else if (type instanceof StructByValue) {
            StructByValue sbv = (StructByValue) type;
            return new StructOp(sbv.getStructClass());
        
        } else if (type instanceof MappedType) {
            return new Mapped(getMemoryOp(((MappedType) type).getRealType(), order), (MappedType) type);
        }

        return null;
    }
    
    abstract IRubyObject get(ThreadContext context, MemoryIO io, long offset);
    abstract void put(ThreadContext context, MemoryIO io, long offset, IRubyObject value);
    
    IRubyObject get(ThreadContext context, AbstractMemory ptr, long offset) {
        return get(context, ptr.getMemoryIO(), offset);
    }

    void put(ThreadContext context, AbstractMemory ptr, long offset, IRubyObject value) {
        put(context, ptr.getMemoryIO(), offset, value);
    }

    static abstract class PrimitiveOp extends MemoryOp {
        abstract IRubyObject get(Ruby runtime, MemoryIO io, long offset);
        abstract void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value);
    
        IRubyObject get(ThreadContext context, MemoryIO io, long offset) {
            return get(context.runtime, io, offset);
        }
        void put(ThreadContext context, MemoryIO io, long offset, IRubyObject value) {
            put(context.runtime, io, offset, value);
        }
    }
    static final class BooleanOp extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putByte(offset, (byte) (value.isTrue() ? 1 : 0));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return runtime.newBoolean(io.getByte(offset) != 0);
        }
    }

    static final class Signed8 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putByte(offset, Util.int8Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned8(runtime, io.getByte(offset));
        }
    }

    static final class Unsigned8 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putByte(offset, (byte) Util.uint8Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned8(runtime, io.getByte(offset));
        }
    }

    static final class Signed16 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, Util.int16Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned16(runtime, io.getShort(offset));
        }
    }

    static final class Signed16Swapped extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, Short.reverseBytes(Util.int16Value(value)));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned16(runtime, Short.reverseBytes(io.getShort(offset)));
        }
    }

    static final class Unsigned16 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, (short) Util.uint16Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned16(runtime, io.getShort(offset));
        }
    }
    
    static final class Unsigned16Swapped extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, Short.reverseBytes((short) Util.uint16Value(value)));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned16(runtime, Short.reverseBytes(io.getShort(offset)));
        }
    }

    static final class Signed32 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, Util.int32Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned32(runtime, io.getInt(offset));
        }
    }

    static final class Signed32Swapped extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, Integer.reverseBytes(Util.int32Value(value)));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned32(runtime, Integer.reverseBytes(io.getInt(offset)));
        }
    }

    static final class Unsigned32 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, (int) Util.uint32Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned32(runtime, io.getInt(offset));
        }
    }

    static final class Unsigned32Swapped extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, Integer.reverseBytes((int) Util.uint32Value(value)));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned32(runtime, Integer.reverseBytes(io.getInt(offset)));
        }
    }
    
    static final class Signed64 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Util.int64Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned64(runtime, io.getLong(offset));
        }
    }

    static final class Signed64Swapped extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Long.reverseBytes(Util.int64Value(value)));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned64(runtime, Long.reverseBytes(io.getLong(offset)));
        }
    }

    static final class Unsigned64 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Util.uint64Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned64(runtime, io.getLong(offset));
        }
    }

    static final class Unsigned64Swapped extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Long.reverseBytes(Util.uint64Value(value)));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned64(runtime, Long.reverseBytes(io.getLong(offset)));
        }
    }
    
    static final class Float32 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            put(runtime.getCurrentContext(), io, offset, value);
        }

        public final void put(ThreadContext context, MemoryIO io, long offset, IRubyObject value) {
            io.putFloat(offset, Util.floatValue(context, value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return runtime.newFloat(io.getFloat(offset));
        }
    }
    
    static final class Float64 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            put(runtime.getCurrentContext(), io, offset, value);
        }

        public final void put(ThreadContext context, MemoryIO io, long offset, IRubyObject value) {
            io.putDouble(offset, Util.doubleValue(context, value));
        }


        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return runtime.newFloat(io.getDouble(offset));
        }
    }

    static final class Float128 extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            return; // not implemented
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return runtime.newFloat(0); // not implemented
        }
    }

    static final class PointerOp extends PrimitiveOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putMemoryIO(offset, ((AbstractMemory) value).getMemoryIO());
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return new Pointer(runtime, io.getMemoryIO(offset));
        }
    }

    
    static final class StructOp extends MemoryOp {
        private final RubyClass structClass;

        public StructOp(RubyClass structClass) {
            this.structClass = structClass;
        }

        @Override
        IRubyObject get(ThreadContext context, MemoryIO io, long offset) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        void put(ThreadContext context, MemoryIO io, long offset, IRubyObject value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        IRubyObject get(ThreadContext context, AbstractMemory ptr, long offset) {
            return structClass.newInstance(context,
                        new IRubyObject[] { ptr.slice(context.runtime, offset) },
                        Block.NULL_BLOCK);
        }

        @Override
        void put(ThreadContext context, AbstractMemory ptr, long offset, IRubyObject value) {
            if (value instanceof Struct s) {
                byte[] tmp = new byte[Struct.getStructSize(context, s)];
                s.getMemoryIO().get(0, tmp, 0, tmp.length);
                ptr.getMemoryIO().put(offset, tmp, 0, tmp.length);
                return;
            }

            throw typeError(context, "expected a struct");
        }
    }
    
    static final class Mapped extends MemoryOp {
        private final MemoryOp nativeOp;
        private final MappedType mappedType;

        public Mapped(MemoryOp nativeOp, MappedType mappedType) {
            this.nativeOp = nativeOp;
            this.mappedType = mappedType;
        }

        @Override
        IRubyObject get(ThreadContext context, AbstractMemory ptr, long offset) {
            return mappedType.fromNative(context, nativeOp.get(context, ptr, offset));
        }

        @Override
        void put(ThreadContext context, AbstractMemory ptr, long offset, IRubyObject value) {
            nativeOp.put(context, ptr, offset, mappedType.toNative(context, value));
        }
        
        @Override
        IRubyObject get(ThreadContext context, MemoryIO io, long offset) {
            return mappedType.fromNative(context, nativeOp.get(context, io, offset));
        }

        @Override
        void put(ThreadContext context, MemoryIO io, long offset, IRubyObject value) {
            nativeOp.put(context, io, offset, mappedType.toNative(context, value));
        }
    }
}
