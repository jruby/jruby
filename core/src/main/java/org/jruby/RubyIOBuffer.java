package org.jruby;

import jnr.ffi.NativeType;
import jnr.ffi.Platform;
import jnr.ffi.Runtime;
import jnr.ffi.Type;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.func.ObjectLongFunction;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.LongUnaryOperator;
import java.util.function.ToLongFunction;

import static org.jruby.RubyBoolean.newBoolean;

public class RubyIOBuffer extends RubyObject {

    public static final Runtime FFI_RUNTIME = Runtime.getSystemRuntime();

    public static RubyClass createIOBufferClass(Ruby runtime) {
        RubyClass IOBuffer = runtime.getIO().defineClassUnder("Buffer", runtime.getObject(), RubyIOBuffer::new);

        IOBuffer.includeModule(runtime.getComparable());

        IOBuffer.defineAnnotatedMethods(RubyIOBuffer.class);
        IOBuffer.defineAnnotatedConstants(RubyIOBuffer.class);

        return IOBuffer;
    }

    @JRubyConstant
    public static final int PAGE_SIZE = 8196;
    @JRubyConstant
    public static final int DEFAULT_SIZE = 8196;
    @JRubyConstant
    public static final int EXTERNAL = 1;
    @JRubyConstant
    public static final int INTERNAL = 2;
    @JRubyConstant
    public static final int MAPPED = 4;
    @JRubyConstant
    public static final int SHARED = 8;
    @JRubyConstant
    public static final int LOCKED = 32;
    @JRubyConstant
    public static final int PRIVATE = 64;
    @JRubyConstant
    public static final int READONLY = 128;
    @JRubyConstant
    public static final int LITTLE_ENDIAN = 4;
    @JRubyConstant
    public static final int BIG_ENDIAN = 8;
    @JRubyConstant
    public static final int HOST_ENDIAN = Platform.getNativePlatform().isBigEndian() ? BIG_ENDIAN : LITTLE_ENDIAN;
    @JRubyConstant
    public static final int NETWORK_ENDIAN = BIG_ENDIAN;

    public static RubyIOBuffer newBuffer(Ruby runtime, ByteBuffer base, int size, int flags) {
        return new RubyIOBuffer(runtime, runtime.getIO(), base, size, flags);
    }

    public RubyIOBuffer(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyIOBuffer(Ruby runtime, RubyClass metaClass, ByteBuffer base, int size, int flags) {
        super(runtime, metaClass);

        this.base = base;
        this.size = size;
        this.flags = flags;
    }

    @JRubyMethod(name = "for")
    public static IRubyObject rbFor(ThreadContext context, IRubyObject self, IRubyObject string) {
        return context.nil;
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context) {
        return initialize(context, DEFAULT_SIZE);
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject size) {
        return initialize(context, size.convertToInteger().getIntValue());
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject _size, IRubyObject flags) {
        IRubyObject nil = context.nil;

        int size = _size.convertToInteger().getIntValue();

        initialize(context, new byte[size], size, flags.convertToInteger().getIntValue(), nil);

        return nil;
    }

    public IRubyObject initialize(ThreadContext context, int size) {
        IRubyObject nil = context.nil;

        initialize(context, new byte[size], size, flagsForSize(size), nil);

        return nil;
    }

    // MRI: io_buffer_initialize
    public void initialize(ThreadContext context, byte[] baseBytes, int size, int flags, IRubyObject source) {
        ByteBuffer base = null;

        if (baseBytes != null) {
            // If we are provided a pointer, we use it.
            base = ByteBuffer.wrap(baseBytes);
        } else if (size != 0) {
            // If we are provided a non-zero size, we allocate it:
            if ((flags & INTERNAL) == INTERNAL) {
                base = ByteBuffer.allocate(size);
            } else if ((flags & MAPPED) == MAPPED) {
                // no support for SHARED, PRIVATE yet
                base = ByteBuffer.allocateDirect(size);
            }

            if (base == null) {
                throw context.runtime.newBufferAllocationError("Could not allocate buffer!");
            }
        } else {
            // Otherwise we don't do anything.
            return;
        }

        this.base = base;
        this.size = size;
        this.flags = flags;
        this.source = source;
    }

    // MRI: io_flags_for_size
    private static int flagsForSize(int size) {
        if (size >= PAGE_SIZE) {
            return MAPPED;
        }

        return INTERNAL;
    }

    @JRubyMethod(name = "initialize_copy")
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        return context.nil;
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "hexdump")
    public IRubyObject hexdump(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        return context.runtime.newFixnum(size);
    }

    @JRubyMethod(name = "valid?")
    public IRubyObject valid_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "transfer")
    public IRubyObject transfer(ThreadContext context) {
        if (isLocked()) {
            throw context.runtime.newBufferLockedError("Cannot transfer ownership of locked buffer!");
        }

        RubyIOBuffer instance = new RubyIOBuffer(context.runtime, getMetaClass());

        instance.base = base;
        instance.size = size;
        instance.flags = flags;
        instance.source = source;

        zero(context);

        return instance;
    }

    private void zero(ThreadContext context) {
        base = null;
        size = 0;
        source = context.nil;
    }

    @JRubyMethod(name = "null?")
    public IRubyObject null_p(ThreadContext context) {
        return newBoolean(context, base == null);
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return newBoolean(context, size == 0);
    }

    @JRubyMethod(name = "external?")
    public IRubyObject external_p(ThreadContext context) {
        return newBoolean(context, isExternal());
    }

    private boolean isExternal() {
        return (flags & EXTERNAL) == EXTERNAL;
    }

    @JRubyMethod(name = "internal?")
    public IRubyObject internal_p(ThreadContext context) {
        return newBoolean(context, isInternal());
    }

    private boolean isInternal() {
        return (flags & INTERNAL) == INTERNAL;
    }

    @JRubyMethod(name = "mapped?")
    public IRubyObject mapped_p(ThreadContext context) {
        return newBoolean(context, isMapped());
    }

    private boolean isMapped() {
        return (flags & MAPPED) == MAPPED;
    }

    @JRubyMethod(name = "shared?")
    public IRubyObject shared_p(ThreadContext context) {
        // no support for shared yet
        return newBoolean(context, false);
    }

    @JRubyMethod(name = "locked?")
    public IRubyObject locked_p(ThreadContext context) {
        return newBoolean(context, isLocked());
    }

    private boolean isLocked() {
        return (flags & LOCKED) == LOCKED;
    }

    @JRubyMethod(name = "readonly?")
    public IRubyObject readonly_p(ThreadContext context) {
        return newBoolean(context, isReadonly());
    }

    private boolean isReadonly() {
        return (flags & READONLY) == READONLY;
    }

    @JRubyMethod(name = "locked")
    public IRubyObject locked(ThreadContext context, Block block) {
        checkLocked(context);

        flags |= LOCKED;

        IRubyObject result = block.yield(context, this);

        flags &= ~LOCKED;

        return result;
    }

    private void checkLocked(ThreadContext context) {
        if (isLocked()) {
            throw context.runtime.newBufferLockedError("Buffer already locked!");
        }
    }

    public IRubyObject lock(ThreadContext context) {
        checkLocked(context);

        flags |= LOCKED;

        return this;
    }

    public IRubyObject unlock(ThreadContext context) {
        if ((flags & LOCKED) == 0) {
            throw context.runtime.newBufferLockedError("Buffer not locked!");
        }

        flags &= ~LOCKED;

        return this;
    }

    private boolean tryUnlock() {
        if (isLocked()) {
            flags &= ~LOCKED;
            return true;
        }

        return false;
    }

    @JRubyMethod(name = "slice")
    public IRubyObject slice(ThreadContext context) {
        return slice(context, 0, size);
    }

    @JRubyMethod(name = "slice")
    public IRubyObject slice(ThreadContext context, IRubyObject _offset) {
        int offset = RubyNumeric.num2int(_offset);

        if (offset < 0) {
            throw context.runtime.newArgumentError("Offset can't be negative!");
        }

        return slice(context, offset, size - offset);
    }

    @JRubyMethod(name = "slice")
    public IRubyObject slice(ThreadContext context, IRubyObject _offset, IRubyObject _length) {
        int offset = RubyNumeric.num2int(_offset);

        if (offset < 0) {
            throw context.runtime.newArgumentError("Offset can't be negative!");
        }
        
        int length = RubyNumeric.num2int(_length);

        if (length < 0) {
            throw context.runtime.newArgumentError("Length can't be negative!");
        }

        return slice(context, offset, length);
    }

    // MRI: rb_io_buffer_slice
    public IRubyObject slice(ThreadContext context, int offset, int length) {
        validateRange(context, offset, length);

        // gross, but slice(int, int) is 13+
        base.position(offset);
        base.limit(offset + length);
        ByteBuffer slice = base.slice();
        base.clear();

        return newBuffer(context.runtime, slice, length, flags);
    }

    // MRI: io_buffer_validate_range
    private void validateRange(ThreadContext context, int offset, int length) {
        if (offset + length > size) {
            throw context.runtime.newArgumentError("Specified offset+length exceeds data size!");
        }
    }

    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return context.runtime.newFixnum(base.compareTo(((RubyIOBuffer) other).base));
    }

    @JRubyMethod(name = "resize")
    public IRubyObject resize(ThreadContext context, IRubyObject size) {
        return context.nil;
    }

    // MRI: rb_io_buffer_resize
    public void resize(ThreadContext context, int size) {
        if (isLocked()) {
            throw context.runtime.newBufferLockedError("Cannot resize locked buffer!");
        }

        if (this.base == null) {
            initialize(context, null, size, flagsForSize(size), context.nil);
            return;
        }

        if (isExternal()) {
            throw context.runtime.newBufferAccessError("Cannot resize external buffer!");
        }

        // no special behavior  for isInternal=true since we do not control the internals of ByteBuffers.

        ByteBuffer newBase = this.base.isDirect() ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
        newBase.put(this.base);

        this.base = newBase;
        this.size = size;
    }

    // MRI: io_buffer_clear
    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context) {
        return clear(context, 0, 0, size);
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject value) {
        return clear(context, RubyNumeric.num2int(value), 0, size);
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject _value, IRubyObject _offset) {
        int value = RubyNumeric.num2int(_value);
        int offset = RubyNumeric.num2int(_offset);
        return clear(context, value, offset, size - offset);
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject _value, IRubyObject _offset, IRubyObject _length) {
        int value = RubyNumeric.num2int(_value);
        int offset = RubyNumeric.num2int(_offset);
        int length = RubyNumeric.num2int(_length);
        return clear(context, value, offset, length);
    }

    // MRI: rb_io_buffer_clear
    private IRubyObject clear(ThreadContext context, int value, int offset, int length) {
        ByteBuffer buffer = getBufferForWriting(context);

        if (offset + length > size) {
            throw context.runtime.newArgumentError("The given offset + length out of bounds!");
        }

        if (buffer.hasArray()) {
            Arrays.fill(buffer.array(), offset, offset + length, (byte) value);
        }

        return this;
    }

    private ByteBuffer getBufferForWriting(ThreadContext context) {
        if (isReadonly()) {
            throw context.runtime.newBufferAccessError("Buffer is not writable!");
        }

        // TODO: validate our buffer

        if (base != null) {
            return base;
        }

        throw context.runtime.newBufferAllocationError("The buffer is not allocated!");
    }

    private ByteBuffer getBufferForReading(ThreadContext context) {
        // TODO: validate our buffer

        if (base != null) {
            return base;
        }

        throw context.runtime.newBufferAllocationError("The buffer is not allocated!");
    }

    @JRubyMethod(name = "free")
    public IRubyObject free(ThreadContext context) {
        if (isLocked()) {
            throw context.runtime.newBufferLockedError("Buffer is locked!");
        }

        freeInternal(context);

        return this;
    }

    private boolean freeInternal(ThreadContext context) {
        if (this.base != null) {
            // No special handling for internal yet

            // No special handling for mapped yet

            // We can only dereference and allow GC to clean it up
            this.base = null;
            this.size = 0;
            this.flags = 0;
            this.source = context.nil;

            return true;
        }

        return false;
    }

    @JRubyMethod(name = "size_of")
    public static IRubyObject size_of(ThreadContext context, IRubyObject self, IRubyObject dataType) {
        if (dataType instanceof RubyArray) {
            long total = 0;
            RubyArray<?> array = (RubyArray<?>) dataType;
            int size = array.size();
            for (int i = 0; i < size; i++) {
                IRubyObject elt = array.eltOk(i);
                total += getDataType(elt).type.size();
            }
        }

        return RubyFixnum.newFixnum(context.runtime, getDataType(dataType).type.size());
    }

    private boolean isBigEndian() {
        return (flags & BIG_ENDIAN) == BIG_ENDIAN;
    }

    private boolean isLittleEndian() {
        return (flags & LITTLE_ENDIAN) == LITTLE_ENDIAN;
    }

    private boolean isHostEndian() {
        return (flags & (BIG_ENDIAN | LITTLE_ENDIAN)) == HOST_ENDIAN;
    }

    private static DataType getDataType(IRubyObject dataType) {
        return DataType.valueOf(RubySymbol.objectToSymbolString(dataType));
    }

    private static byte readByte(ThreadContext context, ByteBuffer buffer, int offset) {
        return buffer.get(offset);
    }

    private static int readUnsignedByte(ThreadContext context, ByteBuffer buffer, int offset) {
        return Byte.toUnsignedInt(buffer.get(offset));
    }

    private static void writeByte(ThreadContext context, ByteBuffer buffer, int offset, byte value) {
        buffer.put(offset, (byte) value);
    }

    private static void writeUnsignedByte(ThreadContext context, ByteBuffer buffer, int offset, int value) {
        buffer.put(offset, (byte) value);
    }

    private static short readShort(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        short s = buffer.getShort(offset);

        if (order == ByteOrder.BIG_ENDIAN) return s;

        return Short.reverseBytes(s);
    }

    private static int readUnsignedShort(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        return Short.toUnsignedInt(readShort(context, buffer, offset, order));
    }

    private static void writeShort(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, short value) {
        if (order == ByteOrder.BIG_ENDIAN) buffer.putShort(offset, value);

        buffer.putShort(offset, Short.reverseBytes(value));
    }

    private static void writeUnsignedShort(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, int value) {
        writeShort(context, buffer, offset, order, (short) value);
    }

    private static int readInt(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        int i = buffer.getInt(offset);

        if (order == ByteOrder.BIG_ENDIAN) return i;

        return Integer.reverseBytes(i);
    }

    private static long readUnsignedInt(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        return Integer.toUnsignedLong(readInt(context, buffer, offset, order));
    }

    private static void writeInt(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, int value) {
        if (order == ByteOrder.BIG_ENDIAN) buffer.putInt(offset, value);

        buffer.putInt(offset, Integer.reverseBytes(value));
    }

    private static void writeUnsignedInt(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, long value) {
        writeInt(context, buffer, offset, order, (int) value);
    }

    private static long readLong(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        long l = buffer.getLong(offset);

        if (order == ByteOrder.BIG_ENDIAN) return l;

        return Long.reverseBytes(l);
    }

    private static BigInteger readUnsignedLong(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        long l = readLong(context, buffer, offset, order);
        if (l > 0L) return BigInteger.valueOf(l);

        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte)(l & 0xFF);
            l >>= 8;
        }

        return new BigInteger(1, bytes);
    }

    private static void writeLong(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, long value) {
        if (order == ByteOrder.BIG_ENDIAN) buffer.putLong(offset, value);

        buffer.putLong(offset, Long.reverseBytes(value));
    }

    private static void writeUnsignedLong(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, long value) {
        writeLong(context, buffer, offset, order, value);
    }

    private static float readFloat(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        float f = buffer.getFloat(offset);

        if (order == ByteOrder.BIG_ENDIAN) return f;

        return Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(f)));
    }

    private static void writeFloat(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, float value) {
        if (order == ByteOrder.BIG_ENDIAN) buffer.putFloat(offset, value);

        buffer.putFloat(offset, Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(value))));
    }

    private static double readDouble(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        double f = buffer.getDouble(offset);

        if (order == ByteOrder.BIG_ENDIAN) return f;

        return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(f)));
    }

    private static void writeDouble(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, double value) {
        if (order == ByteOrder.BIG_ENDIAN) buffer.putDouble(offset, value);

        buffer.putDouble(offset, Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(value))));
    }

    private static IRubyObject wrap(Ruby runtime, long value) {
        return RubyFixnum.newFixnum(runtime, value);
    }

    private static IRubyObject wrap(Ruby runtime, BigInteger value) {
        return RubyBignum.newBignum(runtime, value);
    }

    private static IRubyObject wrap(Ruby runtime, double value) {
        return RubyFloat.newFloat(runtime, value);
    }

    private static long unwrapLong(IRubyObject value) {
        return value.convertToInteger().getLongValue();
    }

    private static double unwrapDouble(IRubyObject value) {
        return value.convertToFloat().getDoubleValue();
    }

    private static long unwrapUnsignedLong(IRubyObject value) {
        return RubyNumeric.num2ulong(value);
    }

    @JRubyMethod(name = "get_value")
    public IRubyObject get_value(ThreadContext context, IRubyObject type, IRubyObject _offset) {
        ByteBuffer buffer = getBufferForReading(context);

        DataType dataType = getDataType(type);
        int offset = RubyNumeric.num2int(_offset);
        int size = this.size;

        return getValue(context, buffer, size, dataType, offset);
    }

    private static IRubyObject getValue(ThreadContext context, ByteBuffer buffer, int size, DataType dataType, int offset) {
        Ruby runtime = context.runtime;

        // TODO: validate size

        switch (dataType) {
            case S8:
                return wrap(runtime, readByte(context, buffer, offset));
            case U8:
                return wrap(runtime, readUnsignedByte(context, buffer, offset));
            case u16:
                return wrap(runtime, readUnsignedShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case U16:
                return wrap(runtime, readUnsignedShort(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case s16:
                return wrap(runtime, readShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case S16:
                return wrap(runtime, readShort(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case u32:
                return wrap(runtime, readUnsignedInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case U32:
                return wrap(runtime, readUnsignedInt(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case s32:
                return wrap(runtime, readInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case S32:
                return wrap(runtime, readInt(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case u64:
                return wrap(runtime, readUnsignedLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case U64:
                return wrap(runtime, readUnsignedLong(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case s64:
                return wrap(runtime, readLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case S64:
                return wrap(runtime, readLong(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case f32:
                return wrap(runtime, readFloat(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case F32:
                return wrap(runtime, readFloat(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case f64:
                return wrap(runtime, readDouble(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case F64:
                return wrap(runtime, readDouble(context, buffer, offset, ByteOrder.BIG_ENDIAN));
        }

        throw runtime.newArgumentError("Unknown data_type: " + dataType); // should never happen
    }

    @JRubyMethod(name = "get_values")
    public IRubyObject get_values(ThreadContext context, IRubyObject dataTypes, IRubyObject _offset) {
        Ruby runtime = context.runtime;

        int offset = RubyNumeric.num2int(_offset);

        int size = this.size;

        ByteBuffer buffer = getBufferForReading(context);

        if (!(dataTypes instanceof RubyArray)) {
            throw runtime.newArgumentError("Argument data_types should be an array!");
        }

        RubyArray<?> dataTypesArray = (RubyArray<?>) dataTypes;
        int dataTypesSize = dataTypesArray.size();
        RubyArray values = RubyArray.newArray(runtime, dataTypesSize);

        for (long i = 0; i < dataTypesSize; i++) {
            IRubyObject type = dataTypesArray.eltOk(i);
            DataType dataType = getDataType(type);

            IRubyObject value = getValue(context, buffer, size, dataType, offset);

            offset += dataType.type.size();

            values.push(value);
        }

        return values;
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject _dataType, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each", _dataType);

        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);

        return each(context, buffer, dataType, 0, size, block);
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject _dataType, IRubyObject _offset, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each", Helpers.arrayOf(_dataType, _offset));

        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);
        int offset = _offset.convertToInteger().getIntValue();

        return each(context, buffer, dataType, offset, size - offset, block);
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject _dataType, IRubyObject _offset, IRubyObject _count, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each", Helpers.arrayOf(_dataType, _offset, _count));

        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);
        int offset = _offset.convertToInteger().getIntValue();
        int count = _count.convertToInteger().getIntValue();

        return each(context, buffer, dataType, offset, count, block);
    }

    private IRubyObject each(ThreadContext context, ByteBuffer buffer, DataType dataType, int offset, int count, Block block) {
        Ruby runtime = context.runtime;

        for (int i = 0 ; i < count; i++) {
            int currentOffset = offset;
            IRubyObject value = getValue(context, buffer, size, dataType, offset);
            offset += dataType.type.size();
            block.yieldSpecific(context, RubyFixnum.newFixnum(runtime, currentOffset), value);
        }

        return this;
    }

    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context, IRubyObject _dataType) {
        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);

        return values(context, buffer, dataType, 0, size);
    }

    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context, IRubyObject _dataType, IRubyObject _offset) {
        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);
        int offset = _offset.convertToInteger().getIntValue();

        return values(context, buffer, dataType, offset, size - offset);
    }

    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context, IRubyObject _dataType, IRubyObject _offset, IRubyObject _count) {
        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);
        int offset = _offset.convertToInteger().getIntValue();
        int count = _count.convertToInteger().getIntValue();

        return values(context, buffer, dataType, offset, count);
    }

    private RubyArray values(ThreadContext context, ByteBuffer buffer, DataType dataType, int offset, int count) {
        Ruby runtime = context.runtime;

        RubyArray values = RubyArray.newArray(runtime, count);

        for (int i = 0 ; i < count; i++) {
            int currentOffset = offset;
            IRubyObject value = getValue(context, buffer, size, dataType, offset);
            offset += dataType.type.size();
            values.push(value);
        }

        return values;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each");

        ByteBuffer buffer = getBufferForReading(context);

        return eachByte(context, buffer, 0, size, block);
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, IRubyObject _offset, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each", Helpers.arrayOf(_offset));

        ByteBuffer buffer = getBufferForReading(context);
        int offset = _offset.convertToInteger().getIntValue();

        return eachByte(context, buffer, offset, size - offset, block);
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, IRubyObject _offset, IRubyObject _count, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each", Helpers.arrayOf(_offset, _count));

        ByteBuffer buffer = getBufferForReading(context);
        int offset = _offset.convertToInteger().getIntValue();
        int count = _count.convertToInteger().getIntValue();

        return eachByte(context, buffer, offset, count, block);
    }

    private IRubyObject eachByte(ThreadContext context, ByteBuffer buffer, int offset, int count, Block block) {
        Ruby runtime = context.runtime;

        for (int i = 0 ; i < count; i++) {
            IRubyObject value = wrap(runtime, readByte(context, buffer, offset));
            block.yieldSpecific(context, value);
        }

        return this;
    }

    private static void setValue(ThreadContext context, ByteBuffer buffer, int size, DataType dataType, int offset, IRubyObject value) {
        // TODO: validate size

        switch (dataType) {
            case S8:
                writeByte(context, buffer, offset, (byte) unwrapLong(value));
            case U8:
                writeUnsignedByte(context, buffer, offset, (int) unwrapLong(value));
            case u16:
                writeUnsignedShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (int) unwrapLong(value));
            case U16:
                writeUnsignedShort(context, buffer, offset, ByteOrder.BIG_ENDIAN, (int) unwrapLong(value));
            case s16:
                writeShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (short) unwrapLong(value));
            case S16:
                writeShort(context, buffer, offset, ByteOrder.BIG_ENDIAN, (short) unwrapLong(value));
            case u32:
                writeUnsignedInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (long) unwrapLong(value));
            case U32:
                writeUnsignedInt(context, buffer, offset, ByteOrder.BIG_ENDIAN, (long) unwrapLong(value));
            case s32:
                writeInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (int) unwrapLong(value));
            case S32:
                writeInt(context, buffer, offset, ByteOrder.BIG_ENDIAN, (int) unwrapLong(value));
            case u64:
                writeUnsignedLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, unwrapUnsignedLong(value));
            case U64:
                writeUnsignedLong(context, buffer, offset, ByteOrder.BIG_ENDIAN, unwrapUnsignedLong(value));
            case s64:
                writeLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, unwrapLong(value));
            case S64:
                writeLong(context, buffer, offset, ByteOrder.BIG_ENDIAN, unwrapLong(value));
            case f32:
                writeFloat(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (float) unwrapDouble(value));
            case F32:
                writeFloat(context, buffer, offset, ByteOrder.BIG_ENDIAN, (float) unwrapDouble(value));
            case f64:
                writeDouble(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, unwrapDouble(value));
            case F64:
                writeDouble(context, buffer, offset, ByteOrder.BIG_ENDIAN, unwrapDouble(value));
        }

        throw context.runtime.newArgumentError("Unknown data_type: " + dataType); // should never happen
    }

    @JRubyMethod(name = "set_value")
    public IRubyObject set_value(ThreadContext context, IRubyObject _dataType, IRubyObject _offset, IRubyObject _value) {
        ByteBuffer buffer = getBufferForWriting(context);

        DataType dataType = getDataType(_dataType);
        int offset = RubyNumeric.num2int(_offset);
        int size = this.size;

        setValue(context, buffer, size, dataType, offset, _value);

        return RubyFixnum.newFixnum(context.runtime, offset + dataType.type.size());
    }

    @JRubyMethod(name = "set_values")
    public IRubyObject set_values(ThreadContext context, IRubyObject _dataTypes, IRubyObject _offset, IRubyObject _values) {
        Ruby runtime = context.runtime;

        int offset = RubyNumeric.num2int(_offset);

        int size = this.size;

        ByteBuffer buffer = getBufferForWriting(context);

        if (!(_dataTypes instanceof RubyArray)) {
            throw runtime.newArgumentError("Argument data_types should be an array!");
        }
        RubyArray<?> dataTypes = (RubyArray<?>) _dataTypes;

        if (!(_values instanceof RubyArray)) {
            throw runtime.newArgumentError("Argument values should be an array!");
        }
        RubyArray<?> values = (RubyArray<?>) _values;

        if (dataTypes.size() != values.size()) {
            throw runtime.newArgumentError("Argument data_types and values should have the same length!");
        }

        int dataTypesSize = dataTypes.size();

        for (long i = 0; i < dataTypesSize; i++) {
            IRubyObject type = dataTypes.eltOk(i);
            DataType dataType = getDataType(type);

            IRubyObject value = values.eltOk(i);

            setValue(context, buffer, size, dataType, offset, value);

            offset += dataType.type.size();
        }

        return RubyFixnum.newFixnum(runtime, offset);
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source) {
        RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;

        return copy(context, sourceBuffer, 0, sourceBuffer.size, 0);
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject _offset) {
        RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;

        int offset = RubyNumeric.num2int(_offset);

        return copy(context, sourceBuffer, offset, sourceBuffer.size, 0);
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject _offset, IRubyObject _length) {
        RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;

        int offset = RubyNumeric.num2int(_offset);
        int length = RubyNumeric.num2int(_length);

        return copy(context, sourceBuffer, offset, length, 0);
    }

    @JRubyMethod(name = "copy", required = 1, optional = 3, checkArity = false)
    public IRubyObject copy(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 3);

        switch (args.length) {
            case 1:
                return copy(context, args[0]);
            case 2:
                return copy(context, args[0], args[1]);
            case 3:
                return copy(context, args[0], args[1], args[2]);
            case 4:
                return copy(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject _offset, IRubyObject _length, IRubyObject _sourceOffset) {
        RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;

        int offset = RubyNumeric.num2int(_offset);
        int length = RubyNumeric.num2int(_length);
        int sourceOffset = RubyNumeric.num2int(_sourceOffset);

        return copy(context, sourceBuffer, offset, length, sourceOffset);
    }

    public IRubyObject copy(ThreadContext context, RubyIOBuffer source, int offset, int length, int sourceOffset) {
        if (sourceOffset > length) {
            throw context.runtime.newArgumentError("The given source offset is bigger than the source itself!");
        }

        ByteBuffer sourceBuffer = source.getBufferForReading(context);

        bufferCopy(context, offset, sourceBuffer, sourceOffset, source.size, length);

        return RubyFixnum.newFixnum(context.runtime, length);
    }

    public IRubyObject copy(ThreadContext context, RubyString source, int offset, int length, int sourceOffset) {
        if (sourceOffset > length) {
            throw context.runtime.newArgumentError("The given source offset is bigger than the source itself!");
        }

        bufferCopy(context, offset, source.getByteList(), sourceOffset, source.size(), length);

        return RubyFixnum.newFixnum(context.runtime, length);
    }

    private void bufferCopy(ThreadContext context, int offset, ByteBuffer sourceBuffer, int sourceOffset, int sourceSize, int length) {
        ByteBuffer destBuffer = getBufferForWriting(context);

        sourceBuffer.position(sourceOffset);
        sourceBuffer.limit(sourceOffset + length);
        if (offset == 0) {
            destBuffer.put(sourceBuffer);
        } else {
            destBuffer.position(offset);
            destBuffer.put(sourceBuffer);
            destBuffer.clear();
        }
        sourceBuffer.clear();
    }

    private void bufferCopy(ThreadContext context, int offset, ByteList sourceBuffer, int sourceOffset, int sourceSize, int length) {
        ByteBuffer destBuffer = getBufferForWriting(context);

        if (offset == 0) {
            destBuffer.put(sourceBuffer.getUnsafeBytes(), sourceBuffer.begin() + sourceOffset, length);
        } else {
            destBuffer.position(offset);
            destBuffer.put(sourceBuffer.getUnsafeBytes(), sourceBuffer.begin() + sourceOffset, length);
            destBuffer.clear();
        }
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject _offset) {
        int offset = RubyNumeric.num2int(_offset);

        return getString(context, offset, size, ASCIIEncoding.INSTANCE);
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject _offset, IRubyObject _length) {
        int offset = RubyNumeric.num2int(_offset);
        int length = RubyNumeric.num2int(_length);

        return getString(context, offset, length, ASCIIEncoding.INSTANCE);
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject _offset, IRubyObject _length, IRubyObject _encoding) {
        int offset = RubyNumeric.num2int(_offset);
        int length = RubyNumeric.num2int(_length);
        Encoding encoding = context.runtime.getEncodingService().getEncodingFromObject(_encoding);

        return getString(context, offset, length, encoding);
    }

    private IRubyObject getString(ThreadContext context, int offset, int length, Encoding encoding) {
        ByteBuffer buffer = getBufferForReading(context);

        validateRange(context, offset, length);

        byte[] bytes = new byte[length];
        if (offset == 0) {
            buffer.get(bytes, 0, length);
        } else {
            buffer.position(offset);
            buffer.get(bytes, 0, length);
            buffer.clear();
        }

        return RubyString.newString(context.runtime, bytes, 0, length, encoding);
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject _string) {
        RubyString string = _string.convertToString();

        return copy(context, string, 0, string.size(), 0);
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject _string, IRubyObject _offset) {
        RubyString string = _string.convertToString();
        int offset = RubyNumeric.num2int(_offset);

        return copy(context, string, offset, string.size(), 0);
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject _string, IRubyObject _offset, IRubyObject _length) {
        RubyString string = _string.convertToString();
        int offset = RubyNumeric.num2int(_offset);
        int length = RubyNumeric.num2int(_length);

        return copy(context, string, offset, length, 0);
    }

    @JRubyMethod(name = "set_string", required = 1, optional = 3, checkArity = false)
    public IRubyObject set_string(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 3);

        switch (args.length) {
            case 1:
                return set_string(context, args[0]);
            case 2:
                return set_string(context, args[0], args[1]);
            case 3:
                return set_string(context, args[0], args[1], args[2]);
            case 4:
                return set_string(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject set_string(ThreadContext context, IRubyObject _string, IRubyObject _offset, IRubyObject _length, IRubyObject _stringOffset) {
        RubyString string = _string.convertToString();
        int offset = RubyNumeric.num2int(_offset);
        int length = RubyNumeric.num2int(_length);
        int stringOffset = RubyNumeric.num2int(_stringOffset);

        return copy(context, string, offset, length, stringOffset);
    }

    @JRubyMethod(name = "&")
    public IRubyObject op_and(ThreadContext context, IRubyObject mask) {
        return context.nil;
    }

    @JRubyMethod(name = "|")
    public IRubyObject op_or(ThreadContext context, IRubyObject mask) {
        return context.nil;
    }

    @JRubyMethod(name = "^")
    public IRubyObject op_xor(ThreadContext context, IRubyObject mask) {
        return context.nil;
    }

    @JRubyMethod(name = "~")
    public IRubyObject op_not(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject io, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject io, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "pread")
    public IRubyObject pread(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "pread", required = 1, optional = 3, checkArity = false)
    public IRubyObject pread(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 3, 4);

        switch (args.length) {
            case 3:
                return pread(context, args[0], args[1], args[2]);
            case 4:
                return pread(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject pread(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject io, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject io, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }
    
    @JRubyMethod(name = "pwrite")
    public IRubyObject pwrite(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "pwrite", required = 1, optional = 3, checkArity = false)
    public IRubyObject pwrite(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 3, 4);

        switch (args.length) {
            case 3:
                return pwrite(context, args[0], args[1], args[2]);
            case 4:
                return pwrite(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject pwrite(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }

    enum DataType {
        U8(NativeType.UCHAR, BIG_ENDIAN),
        S8(NativeType.SCHAR, BIG_ENDIAN),
        u16(NativeType.USHORT, LITTLE_ENDIAN),
        U16(NativeType.USHORT, BIG_ENDIAN),
        s16(NativeType.SSHORT, LITTLE_ENDIAN),
        S16(NativeType.SSHORT, BIG_ENDIAN),
        u32(NativeType.UINT, LITTLE_ENDIAN),
        U32(NativeType.UINT, BIG_ENDIAN),
        s32(NativeType.SINT, LITTLE_ENDIAN),
        S32(NativeType.SINT, BIG_ENDIAN),
        u64(NativeType.ULONG, LITTLE_ENDIAN),
        U64(NativeType.ULONG, BIG_ENDIAN),
        s64(NativeType.SLONG, LITTLE_ENDIAN),
        S64(NativeType.SLONG, BIG_ENDIAN),
        f32(NativeType.FLOAT,LITTLE_ENDIAN),
        F32(NativeType.FLOAT, BIG_ENDIAN),
        f64(NativeType.DOUBLE, LITTLE_ENDIAN),
        F64(NativeType.DOUBLE, BIG_ENDIAN);

        DataType(NativeType type, int endian) {
            this.type = FFI_RUNTIME.findType(type);
            this.endian = endian;
        }

        private final Type type;
        private final int endian;
    }

    private static long swapAsShort(long l) {
        short s = (short) l;

        return (s >>> 8) | (int) (s << 8);
    }

    private static short swap(short s) {
        return (short) ((s >>> 8) | (s << 8));
    }

    private static long swapAsInt(long l) {
        int s = (int) l;

        return (s >>> 16) | (int) (s << 16);
    }

    private static long swapAsLong(long l) {
        return (l >>> 32) | (l << 32);
    }

    private static long swapAsFloat(long l) {
        return swapAsInt(l);
    }

    private static long swapAsDouble(long l) {
        return swapAsLong(l);
    }

    private ByteBuffer base;
    private int size;
    private int flags;
    private IRubyObject source;
}
