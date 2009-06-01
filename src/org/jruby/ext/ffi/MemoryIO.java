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

/**
 * Abstracted memory operations.
 * <p>
 * This abstracts read/write operations to either a native memory area, or a java
 * ByteBuffer.
 * </p>
 */
public interface MemoryIO {
    /**
     * Checks if the memory area is NULL.
     * 
     * @return <tt>true</tt> if the memory area is invalid.
     */
    public boolean isNull();

    /**
     * Checks if the memory area is a native memory pointer.
     *
     * @return <tt>true</tt> if the memory area is a native pointer.
     */
    public boolean isDirect();

    /**
     * Creates a new MemoryIO pointing to a subset of the memory area of this
     * <tt>MemoryIO</tt>.
     * @param offset The offset within the existing memory area to start the
     * new <tt>MemoryIO</tt> at.
     * @return A <tt>MemoryIO</tt> instance.
     */
    public MemoryIO slice(long offset);

    /**
     * Creates a view of this memory object as a java NIO byte buffer.
     *
     * @return A ByteBuffer instance
     */
    public java.nio.ByteBuffer asByteBuffer();
    
    /**
     * Reads an 8 bit integer value from the memory area.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return The 8 bit integer value read from <tt>offset</tt>
     */
    public byte getByte(long offset);
    
    /**
     * Reads a 16 bit integer value from the memory area.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return The 16 bit integer value read from <tt>offset</tt>
     */
    public short getShort(long offset);
    
    /**
     * Reads a 32 bit integer value from the memory area.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return The 32 bit integer value read from <tt>offset</tt>
     */
    public int getInt(long offset);
    
    /**
     * Reads a 64 bit integer value from the memory area.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return The 64 bit integer value read from <tt>offset</tt>
     */
    public long getLong(long offset);
    
    /**
     * Reads a native long integer value from the memory area.
     * <p>
     * A native long is 32bits on either ILP32 or LLP64 architectures, and 
     * 64 bits on an LP64 architecture.
     * </p>
     * <p>
     * This means that it will always read a 32bit value on Windows, but on
     * Unix systems such as MacOS or Linux, it will read a 32bit value on 32bit
     * systems, and a 64bit value on 64bit systems.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return The native long value read from <tt>offset</tt>
     */
    public long getNativeLong(long offset);
    
    /**
     * Reads a float value from the memory area.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return The float value read from <tt>offset</tt>
     */
    public float getFloat(long offset);
    
    /**
     * Reads a double value from the memory area.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return The double value read from <tt>offset</tt>
     */
    public double getDouble(long offset);

     /**
     * Reads a pointer value at the specified offset within the memory area.
     *
     * @param offset The offset within the memory area to read the value.
     * @return A <tt>long</tt> value that represents the address.
     */
    public long getAddress(long offset);

    /**
     * Reads a pointer value at the specified offset within the memory area, and
     * wraps it in an abstract memory accessor.
     * 
     * @param offset The offset within the memory area to read the value.
     * @return A <tt>DirectMemoryIO</tt> accessor that can be used to access the memory
     * pointed to by the address.
     */
    public DirectMemoryIO getMemoryIO(long offset);
    
    /**
     * Writes an 8 bit integer value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The 8 bit integer value to write to the memory location.
     */
    public void putByte(long offset, byte value);
    
    /**
     * Writes a 16 bit integer value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The 16 bit integer value to write to the memory location.
     */
    public void putShort(long offset, short value);
    
    /**
     * Writes a 32 bit integer value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The 32 bit integer value to write to the memory location.
     */
    public void putInt(long offset, int value);
    
    /**
     * Writes a 64 bit integer value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The 64 bit integer value to write to the memory location.
     */
    public void putLong(long offset, long value);
    
    /**
     * Writes a native long integer value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The native long integer value to write to the memory location.
     */
    public void putNativeLong(long offset, long value);
    
    /**
     * Writes a 32 bit float value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The 32 bit float value to write to the memory location.
     */
    public void putFloat(long offset, float value);
    
    /**
     * Writes a 64 bit float value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The 64 bit float value to write to the memory location.
     */
    public void putDouble(long offset, double value);
    
    /**
     * Writes a pointer value to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to write the value.
     * @param value The pointer value to write to the memory location.
     */
    public void putMemoryIO(long offset, MemoryIO value);

    /**
     * Writes a pointer value to the memory area at the specified offset.
     *
     * @param offset The offset within the memory area to write the value.
     * @param value The pointer value to write to the memory location.
     */
    public void putAddress(long offset, long value);
    
    /**
     * Reads an array of bytes from the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to read the bytes.
     * @param dst The output byte array to place the data.
     * @param off The offset within the byte array to start copying.
     * @param len The length of data to read.
     */
    public void get(long offset, byte[] dst, int off, int len);
    
    /**
     * Writes an array of bytes to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to start writing the bytes.
     * @param src The byte array to write to the memory area.
     * @param off The offset within the byte array to start copying.
     * @param len The length of data to write.
     */
    public void put(long offset, byte[] src, int off, int len);

    /**
     * Reads an array of shorts from the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to read the shorts.
     * @param dst The output array to place the data in.
     * @param off The offset within the array to start copying.
     * @param len The number of shorts to read.
     */
    public void get(long offset, short[] dst, int off, int len);

    /**
     * Writes an array of shorts to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to start writing the shorts.
     * @param src The array to write to the memory area.
     * @param off The offset within the array to start copying.
     * @param len The number of shorts to write.
     */
    public void put(long offset, short[] src, int off, int len);
    
    /**
     * Reads an array of ints from the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to read the ints.
     * @param dst The output array to place the data in.
     * @param off The offset within the array to start copying.
     * @param len The number of ints to read.
     */
    public void get(long offset, int[] dst, int off, int len);

    /**
     * Writes an array of ints to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to start writing the ints.
     * @param src The array to write to the memory area.
     * @param off The offset within the array to start copying.
     * @param len The number of ints to write.
     */
    public void put(long offset, int[] src, int off, int len);
    
    /**
     * Reads an array of longs from the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to read the longs.
     * @param dst The output array to place the data in.
     * @param off The offset within the array to start copying.
     * @param len The number of longs to read.
     */
    public void get(long offset, long[] dst, int off, int len);

    /**
     * Writes an array of longs to the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to start writing the longs.
     * @param src The array to write to the memory area.
     * @param off The offset within the array to start copying.
     * @param len The number of longs to write.
     */
    public void put(long offset, long[] src, int off, int len);

    /**
     * Reads an array of floats from the memory area at the specified offset.
     * 
     * @param offset The offset within the memory area to read the floats.
     * @param dst The output array to place the data in.
     * @param off The offset within the array to start copying.
     * @param len The number of floats to read.
     */
    public void get(long offset, float[] dst, int off, int len);

    /**
     * Writes an array of floats to the memory area at the specified offset.
     *
     * @param offset The offset within the memory area to start writing the floats.
     * @param src The array to write to the memory area.
     * @param off The offset within the array to start copying.
     * @param len The number of floats to write.
     */
    public void put(long offset, float[] src, int off, int len);

    /**
     * Reads an array of doubles from the memory area at the specified offset.
     *
     * @param offset The offset within the memory area to read the doubles.
     * @param dst The output array to place the data in.
     * @param off The offset within the array to start copying.
     * @param len The number of doubles to read.
     */
    public void get(long offset, double[] dst, int off, int len);

    /**
     * Writes an array of doubles to the memory area at the specified offset.
     *
     * @param offset The offset within the memory area to start writing the doubles.
     * @param src The array to write to the memory area.
     * @param off The offset within the array to start copying.
     * @param len The number of doubles to write.
     */
    public void put(long offset, double[] src, int off, int len);

    /**
     * Gets the first index within the memory area of a particular 8 bit value.
     * 
     * @param offset The offset within the memory area to start searching.
     * @param value The value to search for.
     * 
     * @return The index of the value, relative to offset.
     */
    public int indexOf(long offset, byte value);
    
    /**
     * Gets the first index within the memory area of a particular 8 bit value.
     * 
     * @param offset The offset within the memory area to start searching.
     * @param value The value to search for.
     * 
     * @return The index of the value, relative to offset.
     */
    public int indexOf(long offset, byte value, int maxlen);

    /**
     * Sets the contents of the memory area to the value.
     * 
     * @param offset The offset within the memory area to start writing.
     * @param size The number of bytes to set to the value.
     * @param value The value to set each byte to.
     */
    public void setMemory(long offset, long size, byte value);

    /**
     * Reads a zero terminated byte array (e.g. an ascii or utf-8 string)
     *
     * @param offset The offset within the memory area of the start of the string.
     * @return A byte array containing a copy of the data.
     */
    public byte[] getZeroTerminatedByteArray(long offset);

    /**
     * Reads a zero terminated byte array (e.g. an ascii or utf-8 string)
     *
     * @param offset The offset within the memory area of the start of the string.
     * @param maxlen The maximum length to search for the zero byte terminator.
     * @return A byte array containing a copy of the data.
     */
    public byte[] getZeroTerminatedByteArray(long offset, int maxlen);

    /**
     * Writes a byte array to memory, and appends a zero terminator
     *
     * @param offset The offset within the memory area of the start of the string.
     * @param bytes The byte array to write to the memory.
     * @param off The offset with the byte array to start copying.
     * @param maxlen The number of bytes of the byte array to write to the memory area. (not including zero byte)
     */
    public void putZeroTerminatedByteArray(long offset, byte[] bytes, int off, int len);
}
