
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

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

    public static final MemoryOp getMemoryOp(NativeType type) {
        switch (type) {
            case BOOL:
                return BOOL;
            case CHAR:
                return INT8;
            case UCHAR:
                return UINT8;
            case SHORT:
                return INT16;
            case USHORT:
                return UINT16;
            case INT:
                return INT32;
            case UINT:
                return UINT32;
            case LONG_LONG:
                return INT64;
            case ULONG_LONG:
                return UINT64;
            case FLOAT:
                return FLOAT32;
            case DOUBLE:
                return FLOAT64;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? INT32 : INT64;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? UINT32 : UINT64;
            default:
                return null;
        }
    }
    
    abstract IRubyObject get(Ruby runtime, MemoryIO io, long offset);
    abstract void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value);

    static final class BooleanOp extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, value.isTrue() ? 1 : 0);
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return runtime.newBoolean(io.getInt(offset) != 0);
        }
    }

    static final class Signed8 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putByte(offset, Util.int8Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned8(runtime, io.getByte(offset));
        }
    }

    static final class Unsigned8 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putByte(offset, (byte) Util.uint8Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned8(runtime, io.getByte(offset));
        }
    }
    static final class Signed16 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, Util.int16Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned16(runtime, io.getShort(offset));
        }
    }
    static final class Unsigned16 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, (short) Util.uint16Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned16(runtime, io.getShort(offset));
        }
    }
    static final class Signed32 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, Util.int32Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned32(runtime, io.getInt(offset));
        }
    }
    static final class Unsigned32 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, (int) Util.uint32Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned32(runtime, io.getInt(offset));
        }
    }
    static final class Signed64 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Util.int64Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned64(runtime, io.getLong(offset));
        }
    }
    static final class Unsigned64 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Util.uint64Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned64(runtime, io.getLong(offset));
        }
    }
    static final class Float32 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putFloat(offset, Util.floatValue(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return runtime.newFloat(io.getFloat(offset));
        }
    }
    static final class Float64 extends MemoryOp {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putDouble(offset, Util.doubleValue(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return runtime.newFloat(io.getDouble(offset));
        }
    }
}
