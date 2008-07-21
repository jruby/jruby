/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 JRuby project
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * C memory pointer operations.
 * <p>
 * This implements the Rubinius FFI MemoryPointer class, but in java for speed.
 * </p>
 */
@JRubyClass(name=AbstractMemoryPointer.className, parent="Object")
public abstract class AbstractMemoryPointer extends RubyObject {
    /** The base class name to register in the Ruby runtime */
    public static final String className = "AbstractPointer";
    /**
     * Used to hold a permanent reference to a memory pointer so it does not get
     * garbage collected
     */
    private static final Map<AbstractMemoryPointer, Object> pointerSet
            = new ConcurrentHashMap();
    /** The offset from the base memory pointer */
    protected final long offset;
    /** The total size of the memory area */
    protected final long size;
    
    public static RubyClass createMemoryPointerClass(Ruby runtime) {
        RubyModule module = runtime.getModule(FFIProvider.MODULE_NAME);
        RubyClass result = module.defineClassUnder(className, runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        
        result.defineAnnotatedMethods(AbstractMemoryPointer.class);
        result.defineAnnotatedConstants(AbstractMemoryPointer.class);

        return result;
    }
    protected AbstractMemoryPointer(Ruby runtime, RubyClass klass) {
        this(runtime, klass, 0, Long.MAX_VALUE);
    }
    protected AbstractMemoryPointer(Ruby runtime, RubyClass klass, long offset, long size) {
        super(runtime, klass);
        this.offset = offset;
        this.size = size;
    }

    /**
     * Gets the memory I/O accessor to read/write to the memory area.
     *
     * @return A memory accessor.
     */
    protected abstract MemoryIO getMemoryIO();

    /**
     * Calculates the absoluate offset within the base memory pointer for a given offset.
     *
     * @param offset The offset to add to the base offset.
     *
     * @return The total offset from the base memory pointer.
     */
    protected final long getOffset(IRubyObject offset) {
        return getOffset() + Util.int64Value(offset);
    }
    
    /**
     * Gets the offset within the memory area.
     *
     * @return The offset within the original memory area.
     */
    protected final long getOffset() {
        return this.offset;
    }

    /**
     * Calculates a hash code for the pointer.
     *
     * @return A RubyFixnum containing the hash code.
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return context.getRuntime().newFixnum(hashCode());
    }

    /**
     * Compares this <tt>MemoryPointer</tt> to another <tt>MemoryPointer</tt>.
     *
     * @param obj The other <tt>MemoryPointer</tt> to compare to.
     * @return true if the memory address of <tt>obj</tt> is equal to the address
     * of this <tt>MemoryPointer</tt>.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractMemoryPointer)) {
            return false;
        }
        final AbstractMemoryPointer other = (AbstractMemoryPointer) obj;
        return other.getMemoryIO().equals(getMemoryIO()) && other.offset == offset;
    }
    
    /**
     * Calculates the hash code for this <tt>MemoryPointer</tt>
     *
     * @return The hashcode of the memory address.
     */
    @Override
    public int hashCode() {
        return 67 * getMemoryIO().hashCode() + (int) (this.offset ^ (this.offset >>> 32));
    }

    /**
     * Gets the total size (in bytes) of the MemoryPointer.
     *
     * @return The total size in bytes.
     */
    @JRubyMethod(name = "total")
    public IRubyObject total(ThreadContext context) {
        return RubyFixnum.newFixnum(context.getRuntime(), size);
    }

    /**
     * Tests if this <tt>MemoryPointer</tt> represents the C <tt>NULL</tt> value.
     *
     * @return true if the address is NULL.
     */
    @JRubyMethod(name = "null?")
    public IRubyObject null_p(ThreadContext context) {
        return context.getRuntime().newBoolean(getMemoryIO().isNull());
    }
    
    /**
     * Reads a pointer value from the memory address.
     *
     * @return A new <tt>MemoryPointer</tt>.
     */
    @JRubyMethod(name = "read_pointer")
    public IRubyObject read_pointer(ThreadContext context) {
        return getMemoryPointer(context.getRuntime(), 0);
    }

    /**
     * Writes a 8 bit signed integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_int8", required = 2)
    public IRubyObject put_int8(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putByte(getOffset(offset), Util.int8Value(value));
        return value;
    }

    /**
     * Reads an 8 bit signed integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_int8", required = 1)
    public IRubyObject get_int8(ThreadContext context, IRubyObject offset) {
        return RubyFixnum.newFixnum(context.getRuntime(), getMemoryIO().getByte(getOffset(offset)));
    }
    
    /**
     * Writes a 8 bit unsigned integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_uint8", required = 2)
    public IRubyObject put_uint8(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putByte(getOffset(offset), (byte) Util.uint8Value(value));
        return this;
    }
    
    /**
     * Reads an 8 bit unsigned integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_uint8", required = 1)
    public IRubyObject get_uint8(ThreadContext context, IRubyObject offset) {
        int value = getMemoryIO().getByte(getOffset(offset));
        return RubyFixnum.newFixnum(context.getRuntime(),
                value < 0 ? (short) ((value & 0x7F) + 0x80) : value);
    }

    /**
     * Writes a 16 bit signed integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_int16", required = 2)
    public IRubyObject put_int16(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putShort(getOffset(offset), Util.int16Value(value));
        return this;
    }

    /**
     * Reads a 16 bit signed integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_int16", required = 1)
    public IRubyObject get_int16(ThreadContext context, IRubyObject offset) {
        return RubyFixnum.newFixnum(context.getRuntime(), getMemoryIO().getShort(getOffset(offset)));
    }
    
    /**
     * Writes a 16 bit unsigned integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_uint16", required = 2)
    public IRubyObject put_uint16(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putShort(getOffset(offset), (short) Util.uint16Value(value));
        return this;
    }

    /**
     * Reads a 16 bit unsigned integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_uint16", required = 1)
    public IRubyObject get_uint16(ThreadContext context, IRubyObject offset) {
        int value = getMemoryIO().getShort(getOffset(offset));
        return RubyFixnum.newFixnum(context.getRuntime(), 
            value < 0 ? (int)((value & 0x7FFF) + 0x8000) : value);
    }
    /**
     * Writes a 32 bit signed integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_int32", required = 2)
    public IRubyObject put_int32(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putInt(getOffset(offset), Util.int32Value(value));
        return this;
    }

    /**
     * Reads a 32 bit signed integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_int32", required = 1)
    public IRubyObject get_int32(ThreadContext context, IRubyObject offset) {
        return RubyFixnum.newFixnum(context.getRuntime(), getMemoryIO().getInt(getOffset(offset)));
    }
    
    /**
     * Writes an 32 bit unsigned integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_uint32", required = 2)
    public IRubyObject put_uint32(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putInt(getOffset(offset), (int) Util.uint32Value(value));
        return this;
    }

    /**
     * Reads a 32 bit unsigned integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_uint32", required = 1)
    public IRubyObject get_uint32(ThreadContext context, IRubyObject offset) {
        long value = getMemoryIO().getInt(getOffset(offset));
        return RubyFixnum.newFixnum(context.getRuntime(), 
                value < 0 ? (long)((value & 0x7FFFFFFFL) + 0x80000000L) : value);
    }
    /**
     * Writes a 64 bit integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_int64", "put_uint64" }, required = 2)
    public IRubyObject put_int64(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putLong(getOffset(offset), Util.int64Value(value));
        return this;
    }
    
    /**
     * Reads a 64 bit integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = {"get_int64", "get_uint64" }, required = 1)
    public IRubyObject get_int64(ThreadContext context, IRubyObject offset) {
        return RubyFixnum.newFixnum(context.getRuntime(), getMemoryIO().getLong(getOffset(offset)));
    }
    
    /**
     * Writes a C long integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_long", required = 2)
    public IRubyObject put_long(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putNativeLong(getOffset(offset), Util.longValue(value));
        return this;
    }
    
    /**
     * Reads a C long integer value from the memory area.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read.
     */
    @JRubyMethod(name = "get_long", required = 1)
    public IRubyObject get_long(ThreadContext context, IRubyObject offset) {
        return RubyFixnum.newFixnum(context.getRuntime(), getMemoryIO().getNativeLong(getOffset(offset)));
    }
    
    /**
     * Writes a C long integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_ulong", required = 2)
    public IRubyObject put_ulong(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putNativeLong(getOffset(offset), Util.longValue(value));
        return this;
    }
    
    /**
     * Reads a C unsigned long integer value from the memory area.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read.
     */
    @JRubyMethod(name = "get_ulong", required = 1)
    public IRubyObject get_ulong(ThreadContext context, IRubyObject offset) {
        long value = getMemoryIO().getNativeLong(getOffset(offset));
        return RubyFixnum.newFixnum(context.getRuntime(),
                value < 0 ? (long)((value & 0x7FFFFFFFL) + 0x80000000L) : value);
    }
    /**
     * Writes an 32 bit floating point value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_float32", required = 2)
    public IRubyObject put_float32(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putFloat(getOffset(offset), Util.floatValue(value));
        return this;
    }

    /**
     * Reads a 32 bit floating point value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_float32", required = 1)
    public IRubyObject get_float32(ThreadContext context, IRubyObject offset) {
        return RubyFloat.newFloat(context.getRuntime(), getMemoryIO().getFloat(getOffset(offset)));
    }
    
    /**
     * Writes an 64 bit floating point value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = "put_float64", required = 2)
    public IRubyObject put_float64(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putDouble(getOffset(offset), Util.doubleValue(value));
        return this;
    }

    /**
     * Reads a 64 bit floating point value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = "get_float64", required = 1)
    public IRubyObject get_float64(ThreadContext context, IRubyObject offset) {
        return RubyFloat.newFloat(context.getRuntime(), getMemoryIO().getDouble(getOffset(offset)));
    }
    
    @JRubyMethod(name = "get_string", required = 1, optional = 1)
    public IRubyObject get_string(ThreadContext context, IRubyObject[] args) {
        long off = getOffset(args[0]);
        int len = 0;
        if (args.length > 1) {
            int maxlen = Util.int32Value(args[1]);
            len = (int) getMemoryIO().indexOf(off, (byte) 0, maxlen);
            if (len < 0 || len > maxlen) {
                len = maxlen;
            }
        } else {
            len = (int) getMemoryIO().indexOf(off, (byte) 0);
        }
        ByteList bl = new ByteList(len);
        getMemoryIO().get(off, bl.unsafeBytes(), bl.begin(), len);
        bl.length(len);
        return context.getRuntime().newString(bl);
    }
    @JRubyMethod(name = "put_string", required = 2, optional = 1)
    public IRubyObject put_string(ThreadContext context, IRubyObject[] args) {
        long off = getOffset(args[0]);
        ByteList bl = args[1].convertToString().getByteList();
        int len = bl.length();
        if (args.length > 2) {
            len = Math.min(Util.int32Value(args[2]) - 1, len);
        }
        getMemoryIO().put(off, bl.unsafeBytes(), bl.begin(), len);
        getMemoryIO().putByte(off + bl.length(), (byte) 0);
        return context.getRuntime().newFixnum(len);
    }
    @JRubyMethod(name = "get_buffer", required = 2)
    public IRubyObject get_buffer(ThreadContext context, IRubyObject off, IRubyObject len_) {
        int len = Util.int32Value(len_);
        ByteList bl = new ByteList(len);
        getMemoryIO().get(getOffset(off), bl.unsafeBytes(), bl.begin(), len);
        bl.length(len);
        return context.getRuntime().newString(bl);
    }
    @JRubyMethod(name = "put_buffer", required = 3)
    public IRubyObject put_buffer(ThreadContext context, IRubyObject off, IRubyObject str, IRubyObject len_) {
        ByteList bl = str.convertToString().getByteList();
        int len = Math.min(bl.length(), Util.int32Value(len_));
        getMemoryIO().put(getOffset(off), bl.unsafeBytes(), bl.begin(), len);
        return context.getRuntime().newFixnum(len);
    }
    
    @JRubyMethod(name = "get_pointer", required = 1)
    public IRubyObject get_pointer(ThreadContext context, IRubyObject offset) {
        return getMemoryPointer(context.getRuntime(), Util.int64Value(offset));
    }
    @JRubyMethod(name = "get_array_of_int16", required = 2)
    public IRubyObject get_array_of_int16(ThreadContext context, IRubyObject offset, IRubyObject length) {
        short[] array = new short[Util.int32Value(length)];
        getMemoryIO().get(Util.int64Value(offset), array, 0, array.length);
        Ruby runtime = context.getRuntime();
        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(RubyFixnum.newFixnum(runtime, array[i]));
        }
        return arr;
    }
    @JRubyMethod(name = "put_array_of_int16", required = 2)
    public IRubyObject put_array_of_int16(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        Ruby runtime = context.getRuntime();
        RubyArray arr = (RubyArray) arrParam;
        short[] array = new short[arr.getLength()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int16Value((IRubyObject) arr.get(i));
        }
        getMemoryIO().put(Util.int64Value(offset), array, 0, array.length);
        return runtime.getNil();
    }
    @JRubyMethod(name = "get_array_of_int32", required = 2)
    public IRubyObject get_array_of_int32(ThreadContext context, IRubyObject offset, IRubyObject length) {
        int[] array = new int[Util.int32Value(length)];
        getMemoryIO().get(Util.int64Value(offset), array, 0, array.length);
        Ruby runtime = context.getRuntime();
        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(RubyFixnum.newFixnum(runtime, array[i]));
        }
        return arr;
    }
    @JRubyMethod(name = "put_array_of_int32", required = 2)
    public IRubyObject put_array_of_int32(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        Ruby runtime = context.getRuntime();
        RubyArray arr = (RubyArray) arrParam;
        int[] array = new int[arr.getLength()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int32Value((IRubyObject) arr.get(i));
        }
        getMemoryIO().put(Util.int64Value(offset), array, 0, array.length);
        return runtime.getNil();
    }
    @JRubyMethod(name = "get_array_of_long", required = 2)
    public IRubyObject get_array_of_long(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return Platform.getPlatform().longSize() == 32
                ? get_array_of_int32(context, offset, length)
                : get_array_of_int64(context, offset, length);
    }
    @JRubyMethod(name = "put_array_of_long", required = 2)
    public IRubyObject put_array_of_long(ThreadContext context, IRubyObject offset, IRubyObject arr) {
        return Platform.getPlatform().longSize() == 32
                ? put_array_of_int32(context, offset, arr)
                : put_array_of_int64(context, offset, arr);
    }
    @JRubyMethod(name = "get_array_of_int64", required = 2)
    public IRubyObject get_array_of_int64(ThreadContext context, IRubyObject offset, IRubyObject length) {
        long[] array = new long[Util.int32Value(length)];
        getMemoryIO().get(Util.int64Value(offset), array, 0, array.length);
        Ruby runtime = context.getRuntime();
        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(RubyFixnum.newFixnum(runtime, array[i]));
        }
        return arr;
    }
    @JRubyMethod(name = "put_array_of_int64", required = 2)
    public IRubyObject put_array_of_int64(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        Ruby runtime = context.getRuntime();
        RubyArray arr = (RubyArray) arrParam;
        long[] array = new long[arr.getLength()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int64Value((IRubyObject) arr.get(i));
        }
        getMemoryIO().put(Util.int64Value(offset), array, 0, array.length);
        return runtime.getNil();
    }
    @JRubyMethod(name = "get_array_of_float", required = 2)
    public IRubyObject get_array_of_float(ThreadContext context, IRubyObject offset, IRubyObject length) {
        float[] array = new float[Util.int32Value(length)];
        getMemoryIO().get(Util.int64Value(offset), array, 0, array.length);
        Ruby runtime = context.getRuntime();
        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(RubyFloat.newFloat(runtime, array[i]));
        }
        return arr;
    }
    @JRubyMethod(name = "put_array_of_float", required = 2)
    public IRubyObject put_array_of_float(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        Ruby runtime = context.getRuntime();
        RubyArray arr = (RubyArray) arrParam;
        float[] array = new float[arr.getLength()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.floatValue((IRubyObject) arr.get(i));
        }
        getMemoryIO().put(Util.int64Value(offset), array, 0, array.length);
        return runtime.getNil();
    }
    @JRubyMethod(name = "get_array_of_double", required = 2)
    public IRubyObject get_array_of_double(ThreadContext context, IRubyObject offset, IRubyObject length) {
        double[] array = new double[Util.int32Value(length)];
        getMemoryIO().get(Util.int64Value(offset), array, 0, array.length);
        Ruby runtime = context.getRuntime();
        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(RubyFloat.newFloat(runtime, array[i]));
        }
        return arr;
    }
    @JRubyMethod(name = "put_array_of_double", required = 2)
    public IRubyObject put_array_of_double(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        Ruby runtime = context.getRuntime();
        RubyArray arr = (RubyArray) arrParam;
        double[] array = new double[arr.getLength()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.doubleValue((IRubyObject) arr.get(i));
        }
        getMemoryIO().put(Util.int64Value(offset), array, 0, array.length);
        return runtime.getNil();
    }
    @JRubyMethod(name = "free")
    public IRubyObject free(ThreadContext context) {
        // Just let the GC collect and free the pointer
        pointerSet.remove(this);
        return context.getRuntime().getNil();
    }
    @JRubyMethod(name = "autorelease=", required = 1)
    public IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        if (release.isTrue()) {
            pointerSet.remove(this);
        } else {
            pointerSet.put(this, Boolean.TRUE);
        }
        return context.getRuntime().getNil();
    }
    abstract protected AbstractMemoryPointer getMemoryPointer(Ruby runtime, long offset);
}
