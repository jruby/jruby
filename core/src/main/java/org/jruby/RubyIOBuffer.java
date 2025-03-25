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
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.OpenFile;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import static org.jruby.api.Access.encodingService;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warnExperimental;

public class RubyIOBuffer extends RubyObject {

    public static final Runtime FFI_RUNTIME = Runtime.getSystemRuntime();

    public static RubyClass createIOBufferClass(ThreadContext context, RubyClass Object, RubyModule Comparable, RubyClass IO) {
        RubyClass IOBuffer = IO.defineClassUnder(context, "Buffer", Object, RubyIOBuffer::new).
                include(context, Comparable).
                defineMethods(context, RubyIOBuffer.class).
                defineConstants(context, RubyIOBuffer.class);

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

    public static RubyIOBuffer newBuffer(ThreadContext context, ByteBuffer base, int size, int flags) {
        if (base == null) return newBuffer(context.runtime, size, flags);

        return new RubyIOBuffer(context.runtime, context.runtime.getIOBuffer(), base, size, flags);
    }

    public static RubyIOBuffer newBuffer(Ruby runtime, ByteBuffer base, int size, int flags) {
        if (base == null) return newBuffer(runtime, size, flags);

        return new RubyIOBuffer(runtime, runtime.getIOBuffer(), base, size, flags);
    }

    public static RubyIOBuffer newBuffer(Ruby runtime, int size, int flags) {
        return new RubyIOBuffer(runtime, runtime.getIOBuffer(), newBufferBase(runtime, size, flags), size, flags);
    }

    public static RubyIOBuffer newBuffer(ThreadContext context, RubyString string, int flags) {
        ByteList bytes = string.getByteList();
        int size = bytes.realSize();

        return newBuffer(context, ByteBuffer.wrap(bytes.unsafeBytes(), bytes.begin(), size), size, flags);
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

    @JRubyMethod(name = "for", meta = true)
    public static IRubyObject rbFor(ThreadContext context, IRubyObject self, IRubyObject _string, Block block) {
        RubyString string = _string.convertToString();
        int flags = string.isFrozen() ? READONLY : 0;

        // If the string is frozen, both code paths are okay.
        // If the string is not frozen, if a block is not given, it must be frozen.
        if (!block.isGiven()) {
            // This internally returns the source string if it's already frozen.
            string = string.newFrozen();
            flags = READONLY;
        } else {
            if ((flags & READONLY) != READONLY) {
                string.modify();
            }
        }

        RubyIOBuffer buffer = newBuffer(context, string, flags);

        if (block.isGiven()) {
            return block.yieldSpecific(context, buffer);
        }

        return buffer;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject string(ThreadContext context, IRubyObject self, IRubyObject _length, Block block) {
        int size = toInt(context, _length);
        if (size < 0) throw argumentError(context, "negative string size (or size too big)");
        RubyString string = newString(context, new byte[size]);
        ByteList bytes = string.getByteList();
        ByteBuffer wrap = ByteBuffer.wrap(bytes.unsafeBytes(), bytes.begin(), size);

        RubyIOBuffer buffer = newBuffer(context, wrap, size, 0);

        block.yieldSpecific(context, buffer);

        return string;
    }

    @JRubyMethod(name = "map", meta = true)
    public static IRubyObject map(ThreadContext context, IRubyObject self, IRubyObject _file) {
        RubyFile file = checkFile(context, _file);

        int size = getSizeFromFile(context, file);

        return map(context, file, size, 0, 0);
    }

    private static RubyFile checkFile(ThreadContext context, IRubyObject _file) {
        RubyIO io = RubyIO.convertToIO(context, _file);

        if (!(io instanceof RubyFile)) throw typeError(context, _file, "File");

        return (RubyFile) io;
    }

    @JRubyMethod(name = "map", meta = true)
    public static IRubyObject map(ThreadContext context, IRubyObject self, IRubyObject _file, IRubyObject _size) {
        RubyFile file = checkFile(context, _file);

        int size = getSizeForMap(context, file, _size);

        return map(context, file, size, 0, 0);
    }

    @JRubyMethod(name = "map", meta = true)
    public static IRubyObject map(ThreadContext context, IRubyObject self, IRubyObject _file, IRubyObject _size, IRubyObject _offset) {
        RubyFile file = checkFile(context, _file);

        int size = getSizeForMap(context, file, _size);

        // This is the file offset, not the buffer offset:
        int offset = toInt(context, _offset);

        return map(context, file, size, offset, 0);
    }

    private static int getSizeForMap(ThreadContext context, RubyFile file, IRubyObject _size) {
        int size;
        if (!_size.isNil()) {
            size = extractSize(context, _size);
        } else {
            size = getSizeFromFile(context, file);
        }
        return size;
    }

    private static int getSizeFromFile(ThreadContext context, RubyFile _file) {
        long file_size = _file.getSize(context);
        if (file_size < 0) throw argumentError(context, "Invalid negative file size!");

        // Here, we assume that file_size is positive:
        if (file_size > Integer.MAX_VALUE) throw argumentError(context, "File larger than address space!");

        // This conversion should be safe:
        return (int) file_size;
    }

    @JRubyMethod(name = "map", required = 1, optional = 3, meta = true)
    public static IRubyObject map(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        ioBufferExperimental(context);

        switch (args.length) {
            case 1:
                return map(context, self, args[0]);
            case 2:
                return map(context, self, args[0], args[1]);
            case 3:
                return map(context, self, args[0], args[1], args[2]);
            case 4:
                return map(context, self, args[0], args[1], args[2], args[3]);
        }
        return context.nil;
    }

    public static IRubyObject map(ThreadContext context, IRubyObject self, IRubyObject _file, IRubyObject _size, IRubyObject _offset, IRubyObject _flags) {
        RubyFile file = checkFile(context, _file);

        int size = getSizeForMap(context, file, _size);

        // This is the file offset, not the buffer offset:
        int offset = toInt(context, _offset);

        int flags = toInt(context, _flags);

        return map(context, file, size, offset, flags);
    }

    private static RubyIOBuffer map(ThreadContext context, RubyFile file, int size, int offset, int flags) {
        RubyIOBuffer buffer = new RubyIOBuffer(context.runtime, context.runtime.getIOBuffer());

        ChannelFD descriptor = file.getOpenFileChecked().fd();

        mapFile(context, buffer, descriptor, size, offset, flags);

        return buffer;
    }

    private static void mapFile(ThreadContext context, RubyIOBuffer buffer, ChannelFD descriptor, int size, int offset, int flags) {
        FileChannel.MapMode protect = FileChannel.MapMode.READ_ONLY;
        int access = 0;

        if ((flags & READONLY) == READONLY) {
            buffer.flags |= READONLY;
        } else {
            protect = FileChannel.MapMode.READ_WRITE;
        }

        if ((flags & PRIVATE) == PRIVATE) {
            buffer.flags |= PRIVATE;
            protect = FileChannel.MapMode.PRIVATE;
        } else {
            // This buffer refers to external buffer.
            buffer.flags |= EXTERNAL;
            buffer.flags |= SHARED;
        }

        if (descriptor.chFile == null) throw typeError(context, "Cannot map non-file resource: " + descriptor.ch);

        ByteBuffer base;

        try {
            base = descriptor.chFile.map(protect, offset, size);
        } catch (IOException ioe) {
            throw Helpers.newIOErrorFromException(context.runtime, ioe);
        }

        buffer.base = base;
        buffer.size = size;

        buffer.flags |= MAPPED;
    }

    public static IRubyObject map(ThreadContext context, IRubyObject self, RubyFile _file, int _size, int _offset, int _flags) {
        return context.nil;
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context) {
        return initialize(context, DEFAULT_SIZE);
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject size) {
        return initialize(context, toInt(context, size));
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject _size, IRubyObject flags) {
        int size = toInt(context, _size);

        initialize(context, new byte[size], size, toInt(context, flags), context.nil);

        return context.nil;
    }

    public IRubyObject initialize(ThreadContext context, int size) {
        initialize(context, new byte[size], size, flagsForSize(size), context.nil);

        return context.nil;
    }

    // MRI: io_buffer_initialize
    public void initialize(ThreadContext context, byte[] baseBytes, int size, int flags, IRubyObject source) {
        ioBufferExperimental(context);

        ByteBuffer base = null;

        if (baseBytes != null) {
            // If we are provided a pointer, we use it.
            base = ByteBuffer.wrap(baseBytes);
        } else if (size != 0) {
            base = newBufferBase(context.runtime, size, flags);
        } else {
            // Otherwise we don't do anything.
            return;
        }

        this.base = base;
        this.size = size;
        this.flags = flags;
        this.source = source.isNil() ? null : source;
    }

    static boolean warned = false;

    private static void ioBufferExperimental(ThreadContext context) {
        if (warned) return;

        warned = true;

        warnExperimental(context, "IO::Buffer is experimental and both the Ruby and native interface may change in the future!");
    }

    private static ByteBuffer newBufferBase(Ruby runtime, int size, int flags) {
        ByteBuffer base;

        // If we are provided a non-zero size, we allocate it:
        if ((flags & INTERNAL) == INTERNAL) {
            base = ByteBuffer.allocate(size);
        } else if ((flags & MAPPED) == MAPPED) {
            // no support for SHARED, PRIVATE yet
            base = ByteBuffer.allocateDirect(size);
        } else {
            throw runtime.newBufferAllocationError("Could not allocate buffer!");
        }

        return base;
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
        RubyIOBuffer otherBuffer = (RubyIOBuffer) other;
        ByteBuffer sourceBase = otherBuffer.getBufferForReading(context);
        int sourceSize = otherBuffer.size;

        initialize(context, null, sourceSize, flagsForSize(size), context.nil);

        return copy(context, otherBuffer, 0, sourceSize, 0);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        RubyString result = to_s(context);

        if (validate()) {
            // Limit the maximum size genearted by inspect.
            if (size <= 256) {
                hexdump(context, result, 16, base, size, false);
            }
        }

        return result;
    }

    private boolean validate() {
        if (source != null) {
            return validateSlice(source, base, size);
        }

        return true;
    }

    private boolean validateSlice(IRubyObject source, ByteBuffer base, int size) {
        ByteBuffer sourceBase = null;
        int sourceSize = 0;

        if (source instanceof RubyString) {
            ByteList sourceBytes = ((RubyString) source).getByteList();
            sourceSize = sourceBytes.getRealSize();
            sourceBase = ByteBuffer.wrap(sourceBytes.getUnsafeBytes(), sourceBytes.begin(), sourceSize);
        } else {
            RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;
            sourceBase = sourceBuffer.base;
            sourceSize = sourceBuffer.size;
        }

        // Source is invalid:
        if (sourceBase == null) return false;

        // Base is out of range:
        if (base.hasArray() && sourceBase.hasArray() && base.array() != sourceBase.array()) return false;

        int sourceEnd = sourceSize;
        int end = size;

        // End is out of range:
        if (end > sourceEnd) return false;

        // It seems okay:
        return true;
    }

    @JRubyMethod(name = "hexdump")
    public IRubyObject hexdump(ThreadContext context) {
        ByteBuffer base = this.base;
        int size = this.size;
        if (validate() && base != null) {
            RubyString result = RubyString.newStringLight(context.runtime, size * 3 + (size / 16) * 12 + 1);

            hexdump(context, result, 16, base, size, true);

            return result;
        }

        return context.nil;
    }

    private static RubyString hexdump(ThreadContext context, RubyString string, int width, ByteBuffer base, int size, boolean first) {
        byte[] text = new byte[width+1];
        text[width] = '\0';

        for (int offset = 0; offset < size; offset += width) {
            Arrays.fill(text, (byte) 0);
            if (first) {
                string.cat("0x".getBytes());
                String hex = String.format("0x%08x ", offset);
                string.cat(hex.getBytes());
                first = false;
            } else {
                string.cat(String.format("\n0x%08x ", offset).getBytes());
            }

            for (int i = 0; i < width; i += 1) {
                if (offset+i < size) {
                    int value = Byte.toUnsignedInt(base.get(offset+i));

                    if (value < 127 && value >= 32) {
                        text[i] = (byte)value;
                    }
                    else {
                        text[i] = '.';
                    }

                    string.cat(String.format("%02x", value).getBytes());
                }
                else {
                    string.cat("   ".getBytes());
                }
            }

            string.cat(' ');
            string.cat(text);
        }

        return string;
    }

    @JRubyMethod(name = "to_s")
    public RubyString to_s(ThreadContext context) {
        RubyString result = newString(context, "#<");

        result.append(getMetaClass().name(context));
        result.cat(String.format(" %d+%d", System.identityHashCode(base), size).getBytes());

        if (base == null) result.cat(" NULL".getBytes());
        if (isExternal()) result.cat(" EXTERNAL".getBytes());
        if (isInternal()) result.cat(" INTERNAL".getBytes());
        if (isMapped()) result.cat(" MAPPED".getBytes());
        if (isShared()) result.cat(" SHARED".getBytes());
        if (isLocked()) result.cat(" LOCKED".getBytes());
        if (isReadonly()) result.cat(" READONLY".getBytes());
        if (source != null) result.cat(" SLICE".getBytes());
        if (!validate()) result.cat(" INVALID".getBytes());

        return result.cat(">".getBytes());
    }

    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        return asFixnum(context, size);
    }

    @JRubyMethod(name = "valid?")
    public IRubyObject valid_p(ThreadContext context) {
        return asBoolean(context, validate());
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
        source = null;
    }

    @JRubyMethod(name = "null?")
    public IRubyObject null_p(ThreadContext context) {
        return asBoolean(context, base == null);
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return asBoolean(context, size == 0);
    }

    @JRubyMethod(name = "external?")
    public IRubyObject external_p(ThreadContext context) {
        return asBoolean(context, isExternal());
    }

    private boolean isExternal() {
        return (flags & EXTERNAL) == EXTERNAL;
    }

    @JRubyMethod(name = "internal?")
    public IRubyObject internal_p(ThreadContext context) {
        return asBoolean(context, isInternal());
    }

    private boolean isInternal() {
        return (flags & INTERNAL) == INTERNAL;
    }

    @JRubyMethod(name = "mapped?")
    public IRubyObject mapped_p(ThreadContext context) {
        return asBoolean(context, isMapped());
    }

    private boolean isMapped() {
        return (flags & MAPPED) == MAPPED;
    }

    @JRubyMethod(name = "shared?")
    public IRubyObject shared_p(ThreadContext context) {
        // no support for shared yet
        return asBoolean(context, false);
    }

    private boolean isShared() {
        return (flags & SHARED) == SHARED;
    }

    @JRubyMethod(name = "locked?")
    public IRubyObject locked_p(ThreadContext context) {
        return asBoolean(context, isLocked());
    }

    private boolean isLocked() {
        return (flags & LOCKED) == LOCKED;
    }

    @JRubyMethod(name = "readonly?")
    public IRubyObject readonly_p(ThreadContext context) {
        return asBoolean(context, isReadonly());
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
        int offset = toInt(context, _offset);
        if (offset < 0) throw argumentError(context, "Offset can't be negative!");

        return slice(context, offset, size - offset);
    }

    @JRubyMethod(name = "slice")
    public IRubyObject slice(ThreadContext context, IRubyObject _offset, IRubyObject _length) {
        int offset = toInt(context, _offset);
        if (offset < 0) throw argumentError(context, "Offset can't be negative!");

        int length = toInt(context, _length);
        if (length < 0) throw argumentError(context, "Length can't be negative!");

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

        return newBuffer(context, slice, length, flags);
    }

    // MRI: io_buffer_validate_range
    private void validateRange(ThreadContext context, int offset, int length) {
        if (offset + length > size) throw argumentError(context, "Specified offset+length is bigger than the buffer size!");
    }

    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return asFixnum(context, base.compareTo(((RubyIOBuffer) other).base));
    }

    @JRubyMethod(name = "resize")
    public IRubyObject resize(ThreadContext context, IRubyObject size) {
        resize(context, toInt(context, size));

        return this;
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
        this.base.limit(Math.min(size, this.base.capacity()));
        newBase.put(this.base);
        newBase.clear();

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
        return clear(context, toInt(context, value), 0, size);
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject _value, IRubyObject _offset) {
        int value = toInt(context, _value);
        int offset = toInt(context, _offset);
        return clear(context, value, offset, size - offset);
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject _value, IRubyObject _offset, IRubyObject _length) {
        int value = toInt(context, _value);
        int offset = toInt(context, _offset);
        int length = toInt(context, _length);
        return clear(context, value, offset, length);
    }

    // MRI: rb_io_buffer_clear
    private IRubyObject clear(ThreadContext context, int value, int offset, int length) {
        ByteBuffer buffer = getBufferForWriting(context);
        if (offset + length > size) throw argumentError(context, "The given offset + length out of bounds!");

        if (buffer.hasArray()) Arrays.fill(buffer.array(), offset, offset + length, (byte) value);

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
            this.source = null;

            return true;
        }

        return false;
    }

    @JRubyMethod(name = "size_of", meta = true)
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

        return asFixnum(context, getDataType(dataType).type.size());
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
        if (order == ByteOrder.BIG_ENDIAN) {
            buffer.putShort(offset, value);
            return;
        }

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
        if (order == ByteOrder.BIG_ENDIAN) {
            buffer.putInt(offset, value);
            return;
        }

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
        if (order == ByteOrder.BIG_ENDIAN) {
            buffer.putLong(offset, value);
            return;
        }

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
        if (order == ByteOrder.BIG_ENDIAN) {
            buffer.putFloat(offset, value);
            return;
        }

        buffer.putFloat(offset, Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(value))));
    }

    private static double readDouble(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order) {
        double f = buffer.getDouble(offset);

        if (order == ByteOrder.BIG_ENDIAN) return f;

        return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(f)));
    }

    private static void writeDouble(ThreadContext context, ByteBuffer buffer, int offset, ByteOrder order, double value) {
        if (order == ByteOrder.BIG_ENDIAN) {
            buffer.putDouble(offset, value);
            return;
        }

        buffer.putDouble(offset, Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(value))));
    }

    private static IRubyObject wrap(ThreadContext context, long value) {
        return asFixnum(context, value);
    }

    private static IRubyObject wrap(ThreadContext context, BigInteger value) {
        return RubyBignum.newBignum(context.runtime, value);
    }

    private static IRubyObject wrap(ThreadContext context, double value) {
        return asFloat(context, value);
    }

    private static double unwrapDouble(ThreadContext context, IRubyObject value) {
        return value.convertToFloat().asDouble(context);
    }

    private static long unwrapUnsignedLong(IRubyObject value) {
        return RubyNumeric.num2ulong(value);
    }

    @JRubyMethod(name = "get_value")
    public IRubyObject get_value(ThreadContext context, IRubyObject type, IRubyObject _offset) {
        ByteBuffer buffer = getBufferForReading(context);

        DataType dataType = getDataType(type);
        int offset = toInt(context, _offset);
        int size = this.size;

        return getValue(context, buffer, size, dataType, offset);
    }

    private static IRubyObject getValue(ThreadContext context, ByteBuffer buffer, int size, DataType dataType, int offset) {
        // TODO: validate size

        switch (dataType) {
            case S8:
                return wrap(context, readByte(context, buffer, offset));
            case U8:
                return wrap(context, readUnsignedByte(context, buffer, offset));
            case u16:
                return wrap(context, readUnsignedShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case U16:
                return wrap(context, readUnsignedShort(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case s16:
                return wrap(context, readShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case S16:
                return wrap(context, readShort(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case u32:
                return wrap(context, readUnsignedInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case U32:
                return wrap(context, readUnsignedInt(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case s32:
                return wrap(context, readInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case S32:
                return wrap(context, readInt(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case u64:
                return wrap(context, readUnsignedLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case U64:
                return wrap(context, readUnsignedLong(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case s64:
                return wrap(context, readLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case S64:
                return wrap(context, readLong(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case f32:
                return wrap(context, readFloat(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case F32:
                return wrap(context, readFloat(context, buffer, offset, ByteOrder.BIG_ENDIAN));
            case f64:
                return wrap(context, readDouble(context, buffer, offset, ByteOrder.LITTLE_ENDIAN));
            case F64:
                return wrap(context, readDouble(context, buffer, offset, ByteOrder.BIG_ENDIAN));
        }

        throw argumentError(context, "Unknown data_type: " + dataType); // should never happen
    }

    @JRubyMethod(name = "get_values")
    public IRubyObject get_values(ThreadContext context, IRubyObject dataTypes, IRubyObject _offset) {
        int offset = toInt(context, _offset);
        int size = this.size;
        ByteBuffer buffer = getBufferForReading(context);

        if (!(dataTypes instanceof RubyArray dataTypesArray)) {
            throw argumentError(context, "Argument data_types should be an array!");
        }

        int dataTypesSize = dataTypesArray.size();
        var values = allocArray(context, dataTypesSize);

        for (long i = 0; i < dataTypesSize; i++) {
            IRubyObject type = dataTypesArray.eltOk(i);
            DataType dataType = getDataType(type);
            IRubyObject value = getValue(context, buffer, size, dataType, offset);

            offset += dataType.type.size();

            values.append(context, value);
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
        int offset = toInt(context, _offset);

        return each(context, buffer, dataType, offset, size - offset, block);
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject _dataType, IRubyObject _offset, IRubyObject _count, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each", Helpers.arrayOf(_dataType, _offset, _count));

        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);
        int offset = toInt(context, _offset);
        int count = toInt(context, _count);

        return each(context, buffer, dataType, offset, count, block);
    }

    private IRubyObject each(ThreadContext context, ByteBuffer buffer, DataType dataType, int offset, int count, Block block) {
        for (int i = 0 ; i < count; i++) {
            int currentOffset = offset;
            IRubyObject value = getValue(context, buffer, size, dataType, offset);
            offset += dataType.type.size();
            block.yieldSpecific(context, asFixnum(context, currentOffset), value);
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
        int offset = toInt(context, _offset);

        return values(context, buffer, dataType, offset, size - offset);
    }

    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context, IRubyObject _dataType, IRubyObject _offset, IRubyObject _count) {
        ByteBuffer buffer = getBufferForReading(context);
        DataType dataType = getDataType(_dataType);
        int offset = toInt(context, _offset);
        int count = toInt(context, _count);

        return values(context, buffer, dataType, offset, count);
    }

    private RubyArray values(ThreadContext context, ByteBuffer buffer, DataType dataType, int offset, int count) {
        var values = allocArray(context, count);

        for (int i = 0 ; i < count; i++) {
            IRubyObject value = getValue(context, buffer, size, dataType, offset);
            offset += dataType.type.size();
            values.push(context, value);
        }

        return values;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each_byte");

        ByteBuffer buffer = getBufferForReading(context);

        return eachByte(context, buffer, 0, size, block);
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, IRubyObject _offset, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each_byte", Helpers.arrayOf(_offset));

        ByteBuffer buffer = getBufferForReading(context);
        int offset = toInt(context, _offset);

        return eachByte(context, buffer, offset, size - offset, block);
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, IRubyObject _offset, IRubyObject _count, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each_byte", Helpers.arrayOf(_offset, _count));

        ByteBuffer buffer = getBufferForReading(context);
        int offset = toInt(context, _offset);
        int count = toInt(context, _count);

        return eachByte(context, buffer, offset, count, block);
    }

    private IRubyObject eachByte(ThreadContext context, ByteBuffer buffer, int offset, int count, Block block) {
        Ruby runtime = context.runtime;

        for (int i = 0 ; i < count; i++) {
            IRubyObject value = wrap(context, readByte(context, buffer, offset + i));
            block.yieldSpecific(context, value);
        }

        return this;
    }

    private static void setValue(ThreadContext context, ByteBuffer buffer, int size, DataType dataType, int offset, IRubyObject value) {
        // TODO: validate size

        switch (dataType) {
            case S8:
                writeByte(context, buffer, offset, (byte) toLong(context, value));
                return;
            case U8:
                writeUnsignedByte(context, buffer, offset, (int) toLong(context, value));
                return;
            case u16:
                writeUnsignedShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (int) toLong(context, value));
                return;
            case U16:
                writeUnsignedShort(context, buffer, offset, ByteOrder.BIG_ENDIAN, (int) toLong(context, value));
                return;
            case s16:
                writeShort(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (short) toLong(context, value));
                return;
            case S16:
                writeShort(context, buffer, offset, ByteOrder.BIG_ENDIAN, (short) toLong(context, value));
                return;
            case u32:
                writeUnsignedInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, toLong(context, value));
                return;
            case U32:
                writeUnsignedInt(context, buffer, offset, ByteOrder.BIG_ENDIAN, toLong(context, value));
                return;
            case s32:
                writeInt(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (int) toLong(context, value));
                return;
            case S32:
                writeInt(context, buffer, offset, ByteOrder.BIG_ENDIAN, (int) toLong(context, value));
                return;
            case u64:
                writeUnsignedLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, unwrapUnsignedLong(value));
                return;
            case U64:
                writeUnsignedLong(context, buffer, offset, ByteOrder.BIG_ENDIAN, unwrapUnsignedLong(value));
                return;
            case s64:
                writeLong(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, toLong(context, value));
                return;
            case S64:
                writeLong(context, buffer, offset, ByteOrder.BIG_ENDIAN, toLong(context, value));
                return;
            case f32:
                writeFloat(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, (float) unwrapDouble(context, value));
                return;
            case F32:
                writeFloat(context, buffer, offset, ByteOrder.BIG_ENDIAN, (float) unwrapDouble(context, value));
                return;
            case f64:
                writeDouble(context, buffer, offset, ByteOrder.LITTLE_ENDIAN, unwrapDouble(context, value));
                return;
            case F64:
                writeDouble(context, buffer, offset, ByteOrder.BIG_ENDIAN, unwrapDouble(context, value));
                return;
        }

        throw argumentError(context, "Unknown data_type: " + dataType); // should never happen
    }

    @JRubyMethod(name = "set_value")
    public IRubyObject set_value(ThreadContext context, IRubyObject _dataType, IRubyObject _offset, IRubyObject _value) {
        ByteBuffer buffer = getBufferForWriting(context);

        DataType dataType = getDataType(_dataType);
        int offset = toInt(context, _offset);
        int size = this.size;

        setValue(context, buffer, size, dataType, offset, _value);

        return asFixnum(context, offset + dataType.type.size());
    }

    @JRubyMethod(name = "set_values")
    public IRubyObject set_values(ThreadContext context, IRubyObject _dataTypes, IRubyObject _offset, IRubyObject _values) {
        int offset = toInt(context, _offset);
        int size = this.size;
        ByteBuffer buffer = getBufferForWriting(context);

        if (!(_dataTypes instanceof RubyArray dataTypes)) {
            throw argumentError(context, "Argument data_types should be an array!");
        }

        if (!(_values instanceof RubyArray values)) {
            throw argumentError(context, "Argument values should be an array!");
        }

        if (dataTypes.size() != values.size()) {
            throw argumentError(context, "Argument data_types and values should have the same length!");
        }

        int dataTypesSize = dataTypes.size();

        for (long i = 0; i < dataTypesSize; i++) {
            IRubyObject type = dataTypes.eltOk(i);
            DataType dataType = getDataType(type);
            IRubyObject value = values.eltOk(i);

            setValue(context, buffer, size, dataType, offset, value);

            offset += dataType.type.size();
        }

        return asFixnum(context, offset);
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source) {
        RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;

        return copy(context, sourceBuffer, 0, sourceBuffer.size, 0);
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject _offset) {
        RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;

        int offset = toInt(context, _offset);

        return copy(context, sourceBuffer, offset, sourceBuffer.size, 0);
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject _offset, IRubyObject _length) {
        RubyIOBuffer sourceBuffer = (RubyIOBuffer) source;

        int offset = toInt(context, _offset);
        int length = toInt(context, _length);

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

        int offset = toInt(context, _offset);
        int length = toInt(context, _length);
        int sourceOffset = toInt(context, _sourceOffset);

        return copy(context, sourceBuffer, offset, length, sourceOffset);
    }

    public IRubyObject copy(ThreadContext context, RubyIOBuffer source, int offset, int length, int sourceOffset) {
        if (sourceOffset > length) {
            throw argumentError(context, "The given source offset is bigger than the source itself!");
        }

        ByteBuffer sourceBuffer = source.getBufferForReading(context);

        bufferCopy(context, offset, sourceBuffer, sourceOffset, source.size, length);

        return asFixnum(context, length);
    }

    // MRI: io_buffer_copy_from
    public IRubyObject copy(ThreadContext context, RubyString source, int offset, int length, int sourceOffset) {
        if (sourceOffset > length) {
            throw argumentError(context, "The given source offset is bigger than the source itself!");
        }

        bufferCopy(context, offset, source.getByteList(), sourceOffset, source.size(), length);

        return asFixnum(context, length);
    }

    private void bufferCopy(ThreadContext context, int offset, ByteBuffer sourceBuffer, int sourceOffset, int sourceSize, int length) {
        ByteBuffer destBuffer = getBufferForWriting(context);

        destBuffer.put(offset, sourceBuffer, sourceOffset, length);
    }

    private void bufferCopy(ThreadContext context, int offset, ByteList sourceBuffer, int sourceOffset, int sourceSize, int length) {
        ByteBuffer destBuffer = getBufferForWriting(context);

        destBuffer.put(offset, sourceBuffer.getUnsafeBytes(), sourceBuffer.begin() + sourceOffset, length);
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context) {
        return getString(context, 0, size, ASCIIEncoding.INSTANCE);
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject _offset) {
        int offset = extractOffset(context, _offset);

        return getString(context, offset, size, ASCIIEncoding.INSTANCE);
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject _offset, IRubyObject _length) {
        int offset = extractOffset(context, _offset);
        int length = extractLength(context, _length, offset);

        return getString(context, offset, length, ASCIIEncoding.INSTANCE);
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject _offset, IRubyObject _length, IRubyObject _encoding) {
        int offset = extractOffset(context, _offset);
        int length = extractLength(context, _length, offset);
        Encoding encoding = encodingService(context).getEncodingFromObject(_encoding);

        return getString(context, offset, length, encoding);
    }

    private IRubyObject getString(ThreadContext context, int offset, int length, Encoding encoding) {
        ByteBuffer buffer = getBufferForReading(context);

        validateRange(context, offset, length);

        byte[] bytes = new byte[length];
        buffer.get(offset, bytes, 0, length);

        return RubyString.newStringNoCopy(context.runtime, bytes, 0, length, encoding);
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject _string) {
        RubyString string = _string.convertToString();

        return copy(context, string, 0, string.size(), 0);
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject _string, IRubyObject _offset) {
        RubyString string = _string.convertToString();
        int offset = extractOffset(context, _offset);

        return copy(context, string, offset, string.size(), 0);
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject _string, IRubyObject _offset, IRubyObject _length) {
        RubyString string = _string.convertToString();
        int offset = extractOffset(context, _offset);
        int length = extractLength(context, _length, offset);

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
        int offset = toInt(context, _offset);
        int length = toInt(context, _length);
        int stringOffset = toInt(context, _stringOffset);

        return copy(context, string, offset, length, stringOffset);
    }

    @JRubyMethod(name = "&")
    public IRubyObject op_and(ThreadContext context, IRubyObject _mask) {
        RubyIOBuffer maskBuffer = castToMaskData(context, _mask);

        checkMask(context, maskBuffer);

        RubyIOBuffer outputBuffer = newBuffer(context.runtime, size, flagsForSize(size));

        bufferAnd(outputBuffer.base, base, size, maskBuffer.base, maskBuffer.size);

        return outputBuffer;
    }

    @JRubyMethod(name = "|")
    public IRubyObject op_or(ThreadContext context, IRubyObject _mask) {
        RubyIOBuffer maskBuffer = castToMaskData(context, _mask);

        checkMask(context, maskBuffer);

        RubyIOBuffer outputBuffer = newBuffer(context.runtime, size, flagsForSize(size));

        bufferOr(outputBuffer.base, base, size, maskBuffer.base, maskBuffer.size);

        return outputBuffer;
    }

    @JRubyMethod(name = "^")
    public IRubyObject op_xor(ThreadContext context, IRubyObject _mask) {
        RubyIOBuffer maskBuffer = castToMaskData(context, _mask);

        checkMask(context, maskBuffer);

        RubyIOBuffer outputBuffer = newBuffer(context.runtime, size, flagsForSize(size));

        bufferXor(outputBuffer.base, base, size, maskBuffer.base, maskBuffer.size);

        return outputBuffer;
    }

    @JRubyMethod(name = "~")
    public IRubyObject op_not(ThreadContext context) {
        RubyIOBuffer outputBuffer = newBuffer(context.runtime, size, flagsForSize(size));

        bufferNot(outputBuffer.base, base, size);

        return outputBuffer;
    }

    @JRubyMethod(name = "and!")
    public IRubyObject and_bang(ThreadContext context, IRubyObject _mask) {
        RubyIOBuffer maskData = castToMaskData(context, _mask);

        checkMask(context, maskData);
        checkOverlaps(context, maskData);

        ByteBuffer base = getBufferForWriting(context);
        ByteBuffer maskBase = maskData.getBufferForReading(context);

        bufferAndInPlace(base, size, maskBase, maskData.size);

        return this;
    }

    @JRubyMethod(name = "or!")
    public IRubyObject or_bang(ThreadContext context, IRubyObject _mask) {
        RubyIOBuffer maskData = castToMaskData(context, _mask);

        checkMask(context, maskData);
        checkOverlaps(context, maskData);

        ByteBuffer base = getBufferForWriting(context);
        ByteBuffer maskBase = maskData.getBufferForReading(context);

        bufferOrInPlace(base, size, maskBase, maskData.size);

        return this;
    }

    @JRubyMethod(name = "xor!")
    public IRubyObject xor_bang(ThreadContext context, IRubyObject _mask) {
        RubyIOBuffer maskData = castToMaskData(context, _mask);

        checkMask(context, maskData);
        checkOverlaps(context, maskData);

        ByteBuffer base = getBufferForWriting(context);
        ByteBuffer maskBase = maskData.getBufferForReading(context);

        bufferXorInPlace(base, size, maskBase, maskData.size);

        return this;
    }

    private static RubyIOBuffer castToMaskData(ThreadContext context, IRubyObject _mask) {
        if (!(_mask instanceof RubyIOBuffer)) throw typeError(context, _mask, context.runtime.getIOBuffer());
        return (RubyIOBuffer) _mask;
    }

    @JRubyMethod(name = "not!")
    public IRubyObject not_bang(ThreadContext context) {
        ByteBuffer base = getBufferForWriting(context);

        bufferNotInPlace(base, size);

        return this;
    }

    private static void checkMask(ThreadContext context, RubyIOBuffer buffer) {
        if (buffer.size == 0) {
            throw context.runtime.newBufferMaskError("Zero-length mask given!");
        }
    }

    private void checkOverlaps(ThreadContext context, RubyIOBuffer other) {
        if (bufferOverlaps(other)) {
            throw context.runtime.newBufferMaskError("Mask overlaps source data!");
        }
    }

    private boolean bufferOverlaps(RubyIOBuffer other) {
        if (base != null && base.hasArray() && other.base != null && other.base.hasArray()) {
            if (base.array() == other.base.array()) {
                return true;
            }
        }

        // unsure how to detect overlap for native buffers
        return false;
    }

    private static void bufferAnd(ByteBuffer output, ByteBuffer base, int size, ByteBuffer mask, int maskSize) {
        for (int offset = 0; offset < size; offset += 1) {
            output.put(offset, (byte) (base.get(offset) & mask.get(offset % maskSize)));
        }
    }

    private static void bufferAndInPlace(ByteBuffer a, int aSize, ByteBuffer b, int bSize) {
        bufferAnd(a, a, aSize, b, bSize);
    }

    private static void bufferOr(ByteBuffer output, ByteBuffer base, int size, ByteBuffer mask, int maskSize) {
        for (int offset = 0; offset < size; offset += 1) {
            output.put(offset, (byte) (base.get(offset) | mask.get(offset % maskSize)));
        }
    }

    private static void bufferOrInPlace(ByteBuffer a, int aSize, ByteBuffer b, int bSize) {
        bufferOr(a, a, aSize, b, bSize);
    }

    private static void bufferXor(ByteBuffer output, ByteBuffer base, int size, ByteBuffer mask, int maskSize) {
        for (int offset = 0; offset < size; offset += 1) {
            output.put(offset, (byte) (base.get(offset) ^ mask.get(offset % maskSize)));
        }
    }

    private static void bufferXorInPlace(ByteBuffer a, int aSize, ByteBuffer b, int bSize) {
        bufferXor(a, a, aSize, b, bSize);
    }

    private static void bufferNot(ByteBuffer output, ByteBuffer base, int size) {
        for (int offset = 0; offset < size; offset += 1) {
            output.put(offset, (byte) ~base.get(offset));
        }
    }

    private static void bufferNotInPlace(ByteBuffer a, int aSize) {
        bufferNot(a, a, aSize);
    }

    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject io) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioRead(context, scheduler, io, this, asFixnum(context, size), asFixnum(context, 0));

            if (result != UNDEF) return result;
        }

        return read(context, io, size, 0);
    }

    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject io, IRubyObject _length) {
        if (_length.isNil()) return read(context, io);

        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger lengthInteger = toInteger(context, _length);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioRead(context, scheduler, io, this, lengthInteger, RubyFixnum.zero(context.runtime));

            if (result != null) return result;
        }

        return read(context, io, lengthInteger.asInt(context), 0);
    }

    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject io, IRubyObject _length, IRubyObject _offset) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger offset = toInteger(context, _offset);

        int length;
        RubyInteger lengthInteger;
        if (_length.isNil()) {
            length = size - offset.asInt(context);
            lengthInteger = null;
        } else {
            lengthInteger = toInteger(context, _length);
            length = lengthInteger.asInt(context);
        }

        if (!scheduler.isNil()) {
            if (lengthInteger == null) lengthInteger = asFixnum(context, length);
            IRubyObject result = FiberScheduler.ioRead(context, scheduler, io, this, lengthInteger, offset);

            if (result != UNDEF) {
                return result;
            }
        }

        return read(context, io, length, offset.asInt(context));
    }

    public IRubyObject read(ThreadContext context, IRubyObject io, int length, int offset) {
        validateRange(context, offset, length);

        ByteBuffer buffer = getBufferForWriting(context);

        return readInternal(context, RubyIO.convertToIO(context, io), buffer, offset, length);
    }

    /**
     * Read from the given io into the given buffer base at the given offset and size limit. The buffer will be left
     * with its position after the last byte read.
     *
     * MRI: io_buffer_read_internal
     *
     * @param context
     * @param io
     * @param base
     * @param offset
     * @param size
     * @return
     */
    private static IRubyObject readInternal(ThreadContext context, RubyIO io, ByteBuffer base, int offset, int size) {
        OpenFile fptr = io.getOpenFileChecked();
        final boolean locked = fptr.lock();
        try {
            base.position(offset);
            base.limit(offset + size);
            int result = OpenFile.readInternal(context, fptr, fptr.fd(), base, offset, size);
            return FiberScheduler.result(context, result, fptr.errno());
        } finally {
            base.clear();
            if (locked) fptr.unlock();
        }
    }

    @JRubyMethod(name = "pread")
    public IRubyObject pread(ThreadContext context, IRubyObject io, IRubyObject _from) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger fromInteger = toInteger(context, _from);
        int offset = 0;
        int length = defaultLength(context, offset);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioPRead(context, scheduler, io, this, fromInteger,
                    asFixnum(context, length), asFixnum(context, 0));

            if (result != UNDEF) return result;
        }

        int from = toInt(context, _from);

        return pread(context, RubyIO.convertToIO(context, io), from, length, offset);
    }

    @JRubyMethod(name = "pread")
    public IRubyObject pread(ThreadContext context, IRubyObject io, IRubyObject _from, IRubyObject _length) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger fromInteger = toInteger(context, _from);
        RubyInteger lengthInteger = toInteger(context, _length);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioPRead(context, scheduler, io, this, fromInteger, lengthInteger, asFixnum(context, 0));
            if (result != UNDEF) return result;
        }

        int from = toInt(context, fromInteger);
        int offset = 0;
        int length = extractLength(context, lengthInteger, offset);

        return pread(context, RubyIO.convertToIO(context, io), from, length, offset);
    }

    @JRubyMethod(name = "pread", required = 2, optional = 2, checkArity = false)
    public IRubyObject pread(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 2, 4);

        switch (args.length) {
            case 2:
                return pread(context, args[0], args[1]);
            case 3:
                return pread(context, args[0], args[1], args[2]);
            case 4:
                return pread(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject pread(ThreadContext context, IRubyObject io, IRubyObject _from, IRubyObject _length, IRubyObject _offset) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger fromInteger = toInteger(context, _from);
        RubyInteger lengthInteger = toInteger(context, _length);
        RubyInteger offsetInteger = toInteger(context, _offset);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioPRead(context, scheduler, io, this, fromInteger, lengthInteger, offsetInteger);
            if (result != UNDEF) return result;
        }

        int from = toInt(context, fromInteger);
        int offset = extractOffset(context, offsetInteger);
        int length = extractLength(context, lengthInteger, offset);

        return pread(context, RubyIO.convertToIO(context, io), from, length, offset);
    }

    public IRubyObject pread(ThreadContext context, RubyIO io, int from, int length, int offset) {
        validateRange(context, offset, length);

        ByteBuffer buffer = getBufferForWriting(context);

        return preadInternal(context, io, buffer, from, offset, length);
    }

    /**
     * Read from the given io into the given buffer base at the given offset, from location, and size limit. The buffer will be left
     * with its position unchanged.
     *
     * MRI: io_buffer_pread_internal
     *
     * @param context
     * @param io
     * @param base
     * @param from
     * @param offset
     * @param size
     * @return
     */
    private static IRubyObject preadInternal(ThreadContext context, RubyIO io, ByteBuffer base, int from, int offset, int size) {
        OpenFile fptr = io.getOpenFileChecked();
        final boolean locked = fptr.lock();
        try {
            base.position(offset);
            base.limit(offset + size);
            int result = OpenFile.preadInternal(context, fptr, fptr.fd(), base, from, size);
            return FiberScheduler.result(context, result, fptr.errno());
        } finally {
            base.clear();
            if (locked) fptr.unlock();
        }
    }

    // MRI: length parts of io_buffer_extract_length_offset and io_buffer_extract_length
    private int extractLength(ThreadContext context, IRubyObject _length, int offset) {
        if (!_length.isNil()) {
            if (RubyNumeric.negativeInt(context, _length)) {
                throw argumentError(context, "Length can't be negative!");
            }

            return toInt(context, _length);
        }

        return defaultLength(context, offset);
    }

    private int defaultLength(ThreadContext context, int offset) {
        if (offset > size) {
            throw argumentError(context, "The given offset is bigger than the buffer size!");
        }

        // Note that the "length" is computed by the size the offset.
        return size - offset;
    }

    // MRI: offset parts of io_buffer_extract_length_offset and io_buffer_extract_offset
    private static int extractOffset(ThreadContext context, IRubyObject _offset) {
        if (RubyNumeric.negativeInt(context, _offset)) {
            throw argumentError(context, "Offset can't be negative!");
        }

        return toInt(context, _offset);
    }

    private static int extractSize(ThreadContext context, IRubyObject _size) {
        if (RubyNumeric.negativeInt(context, _size)) {
            throw argumentError(context, "Size can't be negative!");
        }

        return toInt(context, _size);
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject io) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioWrite(context, scheduler, io, this,
                    asFixnum(context, size), asFixnum(context, 0));

            if (result != null) return result;
        }

        return write(context, io, size, 0);
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject io, IRubyObject length) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger lengthInteger = toInteger(context, length);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioWrite(context, scheduler, io, this, lengthInteger, RubyFixnum.zero(context.runtime));
            if (result != null) return result;
        }

        return write(context, io, lengthInteger.asInt(context), 0);
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject io, IRubyObject length, IRubyObject offset) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger lengthInteger = toInteger(context, length);
        RubyInteger offsetInteger = toInteger(context, offset);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioWrite(context, scheduler, io, this, lengthInteger, offsetInteger);
            if (result != null) return result;
        }

        return write(context, io, lengthInteger.asInt(context), offsetInteger.asInt(context));
    }

    public IRubyObject write(ThreadContext context, IRubyObject io, int length, int offset) {
        validateRange(context, offset, length);

        ByteBuffer buffer = getBufferForReading(context);

        return writeInternal(context, RubyIO.convertToIO(context, io), buffer, offset, length);
    }

    private static IRubyObject writeInternal(ThreadContext context, RubyIO io, ByteBuffer base, int offset, int size) {
        OpenFile fptr = io.getOpenFileChecked();
        final boolean locked = fptr.lock();
        try {
            base.position(offset);
            base.limit(offset + size);
            int result = OpenFile.writeInternal(context, fptr, base, offset, size);
            return FiberScheduler.result(context, result, fptr.errno());
        } finally {
            base.clear();
            if (locked) fptr.unlock();
        }
    }
    
    @JRubyMethod(name = "pwrite")
    public IRubyObject pwrite(ThreadContext context, IRubyObject io, IRubyObject _from) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger fromInteger = toInteger(context, _from);
        int offset = 0;
        int length = defaultLength(context, offset);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioPWrite(context, scheduler, io, this, fromInteger,
                    asFixnum(context, length), asFixnum(context, 0));
            if (result != null) return result;
        }

        int from = toInt(context, fromInteger);

        return pwrite(context, RubyIO.convertToIO(context, io), from, length, offset);
    }

    @JRubyMethod(name = "pwrite")
    public IRubyObject pwrite(ThreadContext context, IRubyObject io, IRubyObject _from, IRubyObject _length) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger fromInteger = toInteger(context, _from);
        RubyInteger lengthInteger = toInteger(context, _length);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioPWrite(context, scheduler, io, this, fromInteger, lengthInteger, RubyFixnum.zero(context.runtime));
            if (result != null) return result;
        }

        int from = toInt(context, fromInteger);
        int offset = 0;
        int length = extractLength(context, lengthInteger, offset);

        return pwrite(context, RubyIO.convertToIO(context, io), from, length, offset);
    }

    @JRubyMethod(name = "pwrite", required = 2, optional = 2, checkArity = false)
    public IRubyObject pwrite(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 2, 4);

        switch (args.length) {
            case 2:
                return pwrite(context, args[0], args[1]);
            case 3:
                return pwrite(context, args[0], args[1], args[2]);
            case 4:
                return pwrite(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject pwrite(ThreadContext context, IRubyObject io, IRubyObject _from, IRubyObject _length, IRubyObject _offset) {
        IRubyObject scheduler = context.getFiberCurrentThread().getSchedulerCurrent();
        RubyInteger fromInteger = toInteger(context, _from);
        RubyInteger lengthInteger = toInteger(context, _length);
        RubyInteger offsetInteger = toInteger(context, _offset);

        if (!scheduler.isNil()) {
            IRubyObject result = FiberScheduler.ioPWrite(context, scheduler, io, this, fromInteger, lengthInteger, offsetInteger);
            if (result != null) return result;
        }

        int from = toInt(context, fromInteger);
        int offset = extractOffset(context, offsetInteger);
        int length = extractLength(context, lengthInteger, offset);

        return pwrite(context, RubyIO.convertToIO(context, io), from, length, offset);
    }

    public IRubyObject pwrite(ThreadContext context, RubyIO io, int from, int length, int offset) {
        validateRange(context, offset, length);

        ByteBuffer buffer = getBufferForReading(context);
        int size = this.size - offset;

        return pwriteInternal(context, RubyIO.convertToIO(context, io), buffer, from, offset, length);
    }

    private static IRubyObject pwriteInternal(ThreadContext context, RubyIO io, ByteBuffer base, int from, int offset, int size) {
        OpenFile fptr = io.getOpenFileChecked();
        final boolean locked = fptr.lock();
        try {
            base.position(offset);
            base.limit(offset + size);
            int result = OpenFile.pwriteInternal(context, fptr, fptr.fd(), base, from, size);
            return FiberScheduler.result(context, result, fptr.errno());
        } finally {
            base.clear();
            if (locked) fptr.unlock();
        }
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
