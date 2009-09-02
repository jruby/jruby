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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * A abstract memory object that defines operations common to both pointers and
 * memory buffers
 */
@JRubyClass(name="FFI::" + AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS, parent="Object")
abstract public class AbstractMemory extends RubyObject {
    public final static String ABSTRACT_MEMORY_RUBY_CLASS = "AbstractMemory";

    /** The total size of the memory area */
    protected long size;

    /** The size of each element of this memory area - e.g. :char is 1, :int is 4 */
    protected int typeSize;

    /** The Memory I/O object */
    protected MemoryIO io;
    
    public static RubyClass createAbstractMemoryClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(ABSTRACT_MEMORY_RUBY_CLASS,
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        
        result.defineAnnotatedMethods(AbstractMemory.class);
        result.defineAnnotatedConstants(AbstractMemory.class);

        return result;
    }
    
    protected static final int calculateSize(ThreadContext context, IRubyObject sizeArg) {
        if (sizeArg instanceof RubyFixnum) {
            return (int) ((RubyFixnum) sizeArg).getLongValue();

        } else if (sizeArg instanceof RubySymbol) {
            return TypeSizeMapper.getTypeSize(context, sizeArg);

        } else if (sizeArg instanceof RubyClass && Struct.isStruct(context.getRuntime(), (RubyClass) sizeArg)) {
            return Struct.getStructSize(context.getRuntime(), sizeArg);

        } else if (sizeArg.respondsTo("size")) {
            return (int) RubyFixnum.num2long(sizeArg.callMethod(context, "size"));

        } else {
            throw context.getRuntime().newArgumentError("Invalid size argument");
        }
    }

    protected static final RubyArray checkArray(IRubyObject obj) {
        if (!(obj instanceof RubyArray)) {
            throw obj.getRuntime().newArgumentError("Array expected");
        }
        return (RubyArray) obj;
    }

    protected AbstractMemory(Ruby runtime, RubyClass klass, MemoryIO io, long size) {
        this(runtime, klass, io, size, 1);
    }

    protected AbstractMemory(Ruby runtime, RubyClass klass, MemoryIO io, long size, int typeSize) {
        super(runtime, klass);
        this.io = io;
        this.size = size;
        this.typeSize = typeSize;
    }

    /**
     * Gets the memory I/O accessor to read/write to the memory area.
     *
     * @return A memory accessor.
     */
    public final MemoryIO getMemoryIO() {
        return io;
    }

    /**
     * Replaces the native memory object backing this ruby memory object
     *
     * @param io The new memory I/O object
     * @return The old memory I/O object
     */
    protected final MemoryIO setMemoryIO(MemoryIO io) {
        MemoryIO old = this.io;
        this.io = io;
        return old;
    }

    /**
     * Calculates the absoluate offset within the base memory pointer for a given offset.
     *
     * @param offset The offset to add to the base offset.
     *
     * @return The total offset from the base memory pointer.
     */
    protected final long getOffset(IRubyObject offset) {
        return Util.longValue(offset);
    }
    
    /**
     * Gets the size of the memory area.
     *
     * @return The size of the memory area.
     */
    public final long getSize() {
        return this.size;
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

    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        return RubyString.newString(context.getRuntime(), ABSTRACT_MEMORY_RUBY_CLASS + "[size=" + size + "]");
    }

    @JRubyMethod(name = "[]")
    public final IRubyObject aref(ThreadContext context, IRubyObject indexArg) {
        final int index = RubyNumeric.num2int(indexArg);
        final int offset = index * typeSize;
        if (offset >= size) {
            throw context.getRuntime().newIndexError(String.format("Index %d out of range", index));
        }
        return slice(context.getRuntime(), offset);
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
        if (!(obj instanceof AbstractMemory)) {
            return false;
        }
        final AbstractMemory other = (AbstractMemory) obj;
        return other.getMemoryIO().equals(getMemoryIO());
    }
    
    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return context.getRuntime().newBoolean(this.equals(obj));
    }
    @Override
    public final boolean eql(IRubyObject other) {
        return this.equals(other);
    }
    /**
     * Calculates the hash code for this <tt>MemoryPointer</tt>
     *
     * @return The hashcode of the memory address.
     */
    @Override
    public int hashCode() {
        return 67 * getMemoryIO().hashCode();
    }

    /**
     * Clears (zeros out) the memory contents.
     */
    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context) {
        getMemoryIO().setMemory(0, size, (byte) 0);
        return this;
    }

    /**
     * Gets the total size (in bytes) of the Memory.
     *
     * @return The total size in bytes.
     */
    @JRubyMethod(name = { "total", "size", "length" })
    public IRubyObject total(ThreadContext context) {
        return RubyFixnum.newFixnum(context.getRuntime(), size);
    }
    
    /**
     * Indicates how many bytes the intrinsic type of the memory uses.
     *
     * @param context
     * @return
     */
    @JRubyMethod(name = "type_size")
    public final IRubyObject type_size(ThreadContext context) {
        return context.getRuntime().newFixnum(typeSize);
    }

    /**
     * Writes a 8 bit signed integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_int8", "put_char" } , required = 2)
    public IRubyObject put_int8(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putByte(getOffset(offset), Util.int8Value(value));

        return this;
    }

    /**
     * Reads an 8 bit signed integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = { "get_int8", "get_char" }, required = 1)
    public IRubyObject get_int8(ThreadContext context, IRubyObject offset) {
        return Util.newSigned8(context.getRuntime(), getMemoryIO().getByte(getOffset(offset)));
    }
    
    /**
     * Writes a 8 bit unsigned integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_uint8", "put_uchar" }, required = 2)
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
    @JRubyMethod(name = { "get_uint8", "get_uchar" }, required = 1)
    public IRubyObject get_uint8(ThreadContext context, IRubyObject offset) {
        return Util.newUnsigned8(context.getRuntime(), getMemoryIO().getByte(getOffset(offset)));
    }

    /**
     * Writes a 16 bit signed integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_int16", "put_short" }, required = 2)
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
    @JRubyMethod(name = { "get_int16", "get_short" }, required = 1)
    public IRubyObject get_int16(ThreadContext context, IRubyObject offset) {
        return Util.newSigned16(context.getRuntime(), getMemoryIO().getShort(getOffset(offset)));
    }
    
    /**
     * Writes a 16 bit unsigned integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_uint16", "put_ushort" }, required = 2)
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
    @JRubyMethod(name = { "get_uint16", "get_ushort" }, required = 1)
    public IRubyObject get_uint16(ThreadContext context, IRubyObject offset) {
        return Util.newUnsigned16(context.getRuntime(), getMemoryIO().getShort(getOffset(offset)));
    }

    /**
     * Writes a 32 bit signed integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_int32", "put_int" }, required = 2)
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
    @JRubyMethod(name = { "get_int32", "get_int" }, required = 1)
    public IRubyObject get_int32(ThreadContext context, IRubyObject offset) {
        return Util.newSigned32(context.getRuntime(), getMemoryIO().getInt(getOffset(offset)));
    }
    
    /**
     * Writes an 32 bit unsigned integer value to the memory address.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_uint32", "put_uint" }, required = 2)
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
    @JRubyMethod(name = { "get_uint32", "get_uint" }, required = 1)
    public IRubyObject get_uint32(ThreadContext context, IRubyObject offset) {
        return Util.newUnsigned32(context.getRuntime(), getMemoryIO().getInt(getOffset(offset)));
    }

    /**
     * Writes a 64 bit integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_int64", "put_long_long" }, required = 2)
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
    @JRubyMethod(name = { "get_int64", "get_long_long" }, required = 1)
    public IRubyObject get_int64(ThreadContext context, IRubyObject offset) {
        return Util.newSigned64(context.getRuntime(), getMemoryIO().getLong(getOffset(offset)));
    }

    /**
     * Writes a 64 bit unsigned integer value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_uint64", "put_ulong_long" }, required = 2)
    public IRubyObject put_uint64(ThreadContext context, IRubyObject offset, IRubyObject value) {
        getMemoryIO().putLong(getOffset(offset), Util.uint64Value(value));

        return this;
    }

    /**
     * Reads a 64 bit unsigned integer value from the memory address.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read from the address.
     */
    @JRubyMethod(name = { "get_uint64", "get_ulong_long" }, required = 1)
    public IRubyObject get_uint64(ThreadContext context, IRubyObject offset) {
        return Util.newUnsigned64(context.getRuntime(), getMemoryIO().getLong(getOffset(offset)));
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
        return Platform.getPlatform().longSize() == 32
                ? put_int32(context, offset, value)
                : put_int64(context, offset, value);
    }
    
    /**
     * Reads a C long integer value from the memory area.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read.
     */
    @JRubyMethod(name = "get_long", required = 1)
    public IRubyObject get_long(ThreadContext context, IRubyObject offset) {
        return Platform.getPlatform().longSize() == 32
                ? get_int32(context, offset)
                : get_int64(context, offset);
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
        return Platform.getPlatform().longSize() == 32
                ? put_uint32(context, offset, value)
                : put_uint64(context, offset, value);
    }
    
    /**
     * Reads a C unsigned long integer value from the memory area.
     *
     * @param offset The offset from the base pointer address to read the value.
     * @return The value read.
     */
    @JRubyMethod(name = "get_ulong", required = 1)
    public IRubyObject get_ulong(ThreadContext context, IRubyObject offset) {
        return Platform.getPlatform().longSize() == 32
                ? get_uint32(context, offset)
                : get_uint64(context, offset);
    }
    /**
     * Writes an 32 bit floating point value to the memory area.
     *
     * @param offset The offset from the base pointer address to write the value.
     * @param value The value to write.
     * @return The value written.
     */
    @JRubyMethod(name = { "put_float32", "put_float" }, required = 2)
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
    @JRubyMethod(name = { "get_float32", "get_float" }, required = 1)
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
    @JRubyMethod(name = { "put_float64", "put_double" }, required = 2)
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
    @JRubyMethod(name = { "get_float64", "get_double" }, required = 1)
    public IRubyObject get_float64(ThreadContext context, IRubyObject offset) {
        return RubyFloat.newFloat(context.getRuntime(), getMemoryIO().getDouble(getOffset(offset)));
    }

    /**
     * Reads an array of signed 8 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_int8", "get_array_of_char" }, required = 2)
    public IRubyObject get_array_of_int8(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfSigned8(context.getRuntime(), io, getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of signed 8 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_int8", "put_array_of_char" }, required = 2)
    public IRubyObject put_array_of_int8(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        
        MemoryUtil.putArrayOfSigned8(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of unsigned 8 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_uint8", "get_array_of_uchar" }, required = 2)
    public IRubyObject get_array_of_uint8(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfUnsigned8(context.getRuntime(), io, getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of unsigned 8 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_uint8", "put_array_of_uchar" }, required = 2)
    public IRubyObject put_array_of_uint8(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {

        MemoryUtil.putArrayOfUnsigned8(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of signed 16 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_int16", "get_array_of_short" }, required = 2)
    public IRubyObject get_array_of_int16(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfSigned16(context.getRuntime(), getMemoryIO(), getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of signed 16 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_int16", "put_array_of_short" }, required = 2)
    public IRubyObject put_array_of_int16(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        
        MemoryUtil.putArrayOfSigned16(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));
        
        return this;
    }

    /**
     * Reads an array of unsigned 16 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_uint16", "get_array_of_ushort" }, required = 2)
    public IRubyObject get_array_of_uint16(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfUnsigned16(context.getRuntime(), getMemoryIO(), getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of unsigned 16 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_uint16", "put_array_of_ushort" }, required = 2)
    public IRubyObject put_array_of_uint16(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {

        MemoryUtil.putArrayOfUnsigned16(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of signed 32 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_int32", "get_array_of_int" }, required = 2)
    public IRubyObject get_array_of_int32(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfSigned32(context.getRuntime(), getMemoryIO(), getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of signed 32 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_int32", "put_array_of_int" }, required = 2)
    public IRubyObject put_array_of_int32(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        MemoryUtil.putArrayOfSigned32(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of unsigned 32 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_uint32", "get_array_of_uint" }, required = 2)
    public IRubyObject get_array_of_uint32(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfUnsigned32(context.getRuntime(), getMemoryIO(), getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of unsigned 32 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_uint32", "put_array_of_uint" }, required = 2)
    public IRubyObject put_array_of_uint32(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        MemoryUtil.putArrayOfUnsigned32(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of signed long integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = "get_array_of_long", required = 2)
    public IRubyObject get_array_of_long(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return Platform.getPlatform().longSize() == 32
                ? get_array_of_int32(context, offset, length)
                : get_array_of_int64(context, offset, length);
    }

    /**
     * Writes an array of signed long integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = "put_array_of_long", required = 2)
    public IRubyObject put_array_of_long(ThreadContext context, IRubyObject offset, IRubyObject arr) {
        return Platform.getPlatform().longSize() == 32
                ? put_array_of_int32(context, offset, arr)
                : put_array_of_int64(context, offset, arr);
    }

    /**
     * Reads an array of unsigned long integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = "get_array_of_ulong", required = 2)
    public IRubyObject get_array_of_ulong(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return Platform.getPlatform().longSize() == 32
                ? get_array_of_uint32(context, offset, length)
                : get_array_of_uint64(context, offset, length);
    }

    /**
     * Writes an array of unsigned long integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = "put_array_of_ulong", required = 2)
    public IRubyObject put_array_of_ulong(ThreadContext context, IRubyObject offset, IRubyObject arr) {
        return Platform.getPlatform().longSize() == 32
                ? put_array_of_uint32(context, offset, arr)
                : put_array_of_uint64(context, offset, arr);
    }

    /**
     * Reads an array of signed 64 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_int64", "get_array_of_long_long" }, required = 2)
    public IRubyObject get_array_of_int64(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfSigned64(context.getRuntime(), io, getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of signed 64 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_int64", "put_array_of_long_long" }, required = 2)
    public IRubyObject put_array_of_int64(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        
        MemoryUtil.putArrayOfSigned64(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of unsigned 64 bit integer values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_uint64", "get_array_of_ulong_long" }, required = 2)
    public IRubyObject get_array_of_uint64(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfUnsigned64(context.getRuntime(), io, getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of unsigned 64 bit integer values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_uint64", "put_array_of_ulong_long" }, required = 2)
    public IRubyObject put_array_of_uint64(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {

        MemoryUtil.putArrayOfUnsigned64(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of signed 32 bit floating point values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_float32", "get_array_of_float" }, required = 2)
    public IRubyObject get_array_of_float(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfFloat32(context.getRuntime(), io, getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of 32 bit floating point values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_float32", "put_array_of_float" }, required = 2)
    public IRubyObject put_array_of_float(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {

        MemoryUtil.putArrayOfFloat32(context.getRuntime(), io, getOffset(offset), checkArray(arrParam));

        return this;
    }

    /**
     * Reads an array of signed 64 bit floating point values from the memory address.
     *
     * @param offset The offset from the start of the memory area to read the values.
     * @param length The number of values to be read from memory.
     * @return An array containing the values.
     */
    @JRubyMethod(name = { "get_array_of_float64", "get_array_of_double" }, required = 2)
    public IRubyObject get_array_of_float64(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return MemoryUtil.getArrayOfFloat64(context.getRuntime(), io, getOffset(offset), Util.int32Value(length));
    }

    /**
     * Writes an array of 64 bit floating point values to the memory area.
     *
     * @param offset The offset from the start of the memory area to write the values.
     * @param length The number of values to be written to memory.
     * @return <tt>this</tt> object.
     */
    @JRubyMethod(name = { "put_array_of_float64", "put_array_of_double" }, required = 2)
    public IRubyObject put_array_of_float64(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        MemoryUtil.putArrayOfFloat32(context.getRuntime(), getMemoryIO(), getOffset(offset), checkArray(arrParam));

        return this;
    }

    @JRubyMethod(name = "read_string")
    public IRubyObject read_string(ThreadContext context) {
        return MemoryUtil.getTaintedString(context.getRuntime(), getMemoryIO(), 0);
    }

    @JRubyMethod(name = "read_string")
    public IRubyObject read_string(ThreadContext context, IRubyObject rbLength) {
        /* When a length is given, read_string acts like get_bytes */
        return !rbLength.isNil()
                ? MemoryUtil.getTaintedByteString(context.getRuntime(), getMemoryIO(), 0, Util.int32Value(rbLength))
                : MemoryUtil.getTaintedString(context.getRuntime(), getMemoryIO(), 0);
    }

    @JRubyMethod(name = "get_string", required = 1)
    public IRubyObject get_string(ThreadContext context, IRubyObject offArg) {
        return MemoryUtil.getTaintedString(context.getRuntime(), getMemoryIO(), getOffset(offArg));
    }

    @JRubyMethod(name = "get_string", required = 2)
    public IRubyObject get_string(ThreadContext context, IRubyObject offArg, IRubyObject lenArg) {
        return MemoryUtil.getTaintedString(context.getRuntime(), getMemoryIO(),
                getOffset(offArg), Util.int32Value(lenArg));
    }

    @JRubyMethod(name = { "get_array_of_string" }, required = 1)
    public IRubyObject get_array_of_string(ThreadContext context, IRubyObject rbOffset) {
        final int POINTER_SIZE = (Platform.getPlatform().addressSize() / 8);
        
        final Ruby runtime = context.getRuntime();
        final RubyArray arr = RubyArray.newArray(runtime);

        for (long off = getOffset(rbOffset); off <= size - POINTER_SIZE; off += POINTER_SIZE) {
            final MemoryIO mem = getMemoryIO().getMemoryIO(off);
            if (mem == null || mem.isNull()) {
                break;
            }
            arr.add(MemoryUtil.getTaintedString(runtime, mem, 0));
        }

        return arr;
    }

    @JRubyMethod(name = { "get_array_of_string" }, required = 2)
    public IRubyObject get_array_of_string(ThreadContext context, IRubyObject rbOffset, IRubyObject rbCount) {
        final int POINTER_SIZE = (Platform.getPlatform().addressSize() / 8);
        final long off = getOffset(rbOffset);
        final int count = Util.int32Value(rbCount);

        final Ruby runtime = context.getRuntime();
        final RubyArray arr = RubyArray.newArray(runtime, count);

        for (int i = 0; i < count; ++i) {
            final MemoryIO mem = getMemoryIO().getMemoryIO(off + (i * POINTER_SIZE));
            arr.add(mem != null && !mem.isNull()
                    ? MemoryUtil.getTaintedString(runtime, mem, 0)
                    : runtime.getNil());
        }

        return arr;
    }

    @JRubyMethod(name = "put_string")
    public IRubyObject put_string(ThreadContext context, IRubyObject offArg, IRubyObject strArg) {
        long off = getOffset(offArg);
        ByteList bl = strArg.convertToString().getByteList();
        getMemoryIO().putZeroTerminatedByteArray(off, bl.unsafeBytes(), bl.begin(), bl.length());
        return this;
    }

    @JRubyMethod(name = "get_bytes")
    public IRubyObject get_bytes(ThreadContext context, IRubyObject offArg, IRubyObject lenArg) {
        return MemoryUtil.getTaintedByteString(context.getRuntime(), getMemoryIO(),
                getOffset(offArg), Util.int32Value(lenArg));
    }

    @JRubyMethod(name = "put_bytes", required = 2, optional = 2)
    public IRubyObject put_bytes(ThreadContext context, IRubyObject[] args) {
        long off = getOffset(args[0]);
        ByteList bl = args[1].convertToString().getByteList();
        int idx = args.length > 2 ? Util.int32Value(args[2]) : 0;
        if (idx < 0 || idx > bl.length()) {
            throw context.getRuntime().newRangeError("Invalid string index");
        }
        int len = args.length > 3 ? Util.int32Value(args[3]) : (bl.length() - idx);
        if (len < 0 || len > (bl.length() - idx)) {
            throw context.getRuntime().newRangeError("Invalid length");
        }
        getMemoryIO().put(off, bl.unsafeBytes(), bl.begin() + idx, len);
        return this;
    }
    @JRubyMethod(name = "get_pointer", required = 1)
    public IRubyObject get_pointer(ThreadContext context, IRubyObject offset) {
        return getPointer(context.getRuntime(), getOffset(offset));
    }

    private void putPointer(ThreadContext context, long offset, IRubyObject value) {
        if (value instanceof Pointer) {
            putPointer(context, offset, (Pointer) value);
        } else if (value.isNil()) {
            getMemoryIO().putAddress(offset, 0L);
        } else if (value.respondsTo("to_ptr")) {
            putPointer(context, offset, value.callMethod(context, "to_ptr"));
        } else {
            throw context.getRuntime().newTypeError(value, context.getRuntime().fastGetModule("FFI").fastGetClass("Pointer"));
        }
    }

    private void putPointer(ThreadContext context, long offset, Pointer value) {
        MemoryIO ptr = value.getMemoryIO();
        if (ptr.isDirect()) {
            getMemoryIO().putMemoryIO(offset, ptr);
        } else if (ptr.isNull()) {
            getMemoryIO().putAddress(offset, 0L);
        } else {
            throw context.getRuntime().newArgumentError("Cannot convert argument to pointer");
        }
    }

    @JRubyMethod(name = "put_pointer", required = 2)
    public IRubyObject put_pointer(ThreadContext context, IRubyObject offset, IRubyObject value) {
        putPointer(context, getOffset(offset), value);
        return this;
    }

    @JRubyMethod(name = { "get_array_of_pointer" }, required = 2)
    public IRubyObject get_array_of_pointer(ThreadContext context, IRubyObject offset, IRubyObject length) {
        final int POINTER_SIZE = (Platform.getPlatform().addressSize / 8);
        int count = Util.int32Value(length);
        Ruby runtime = context.getRuntime();
        RubyArray arr = RubyArray.newArray(runtime, count);
        long off = getOffset(offset);

        for (int i = 0; i < count; ++i) {
            arr.add(getPointer(runtime, off + (i * POINTER_SIZE)));
        }

        return arr;
    }

    @JRubyMethod(name = { "put_array_of_pointer" }, required = 2)
    public IRubyObject put_array_of_pointer(ThreadContext context, IRubyObject offset, IRubyObject arrParam) {
        final int POINTER_SIZE = (Platform.getPlatform().addressSize / 8);
        final RubyArray arr = (RubyArray) arrParam;
        final int count = arr.getLength();

        long off = getOffset(offset);
        for (int i = 0; i < count; ++i) {
            putPointer(context, off + (i * POINTER_SIZE), arr.entry(i));
        }
        return this;
    }

    @JRubyMethod(name = "put_callback", required = 3)
    public IRubyObject put_callback(ThreadContext context, IRubyObject offset, IRubyObject proc, IRubyObject cbInfo) {
        if (!(cbInfo instanceof CallbackInfo)) {
            throw context.getRuntime().newArgumentError("invalid CallbackInfo");
        }
        Pointer ptr = Factory.getInstance().getCallbackManager().getCallback(context.getRuntime(), (CallbackInfo) cbInfo, proc);
        getMemoryIO().putMemoryIO(getOffset(offset), ((AbstractMemory) ptr).getMemoryIO());
        return this;
    }
    
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(ThreadContext context, IRubyObject value) {
        return slice(context.getRuntime(), RubyNumeric.fix2long(value));
    }
    abstract protected AbstractMemory slice(Ruby runtime, long offset);
    abstract protected Pointer getPointer(Ruby runtime, long offset);
}
