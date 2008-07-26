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

package org.jruby.ext.ffi.jna;

import org.jruby.ext.ffi.*;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * JNA implementation of memory I/O operations.
 */
public abstract class JNAMemoryIO implements MemoryIO {
    /**
     * The native memory pointer
     */
    final Object memory;

    /**
     * NULL Pointer MemoryIO
     */
    static final JNAMemoryIO NULL = new PointerIO(Pointer.NULL, 0);

    /**
     * Allocates a new block of java heap memory and wraps it in a {@link MemoryIO}
     * accessor.
     * 
     * @param size The size in bytes of memory to allocate.
     * 
     * @return A new <tt>MemoryIO</tt> instance that can access the memory.
     */
    static JNAMemoryIO allocate(int size) {
        return BufferIO.allocate(size);
    }
    
    /**
     * Allocates a new block of native memory and wraps it in a {@link MemoryIO}
     * accessor.
     * 
     * @param size The size in bytes of memory to allocate.
     * 
     * @return A new <tt>MemoryIO</tt> instance that can access the memory.
     */
    static JNAMemoryIO allocateDirect(int size) {
        return PointerIO.allocate(size);
    }
    
    /**
     * Creates a new JNA <tt>MemoryIO</tt> instance.
     * 
     * @param memory The memory object to wrap.
     */
    JNAMemoryIO(Object memory) {
        this.memory = memory;
    }
    
    /**
     * Gets the underlying memory object this <tt>MemoryIO</tt> is wrapping.
     * 
     * @return The native pointer or ByteBuffer.
     */
    Object getMemory() {
        return memory;
    }
    
    /**
     * Wraps a <tt>MemoryIO</tt> accessor around an existing native memory area.
     * 
     * @param ptr The native pointer to wrap.
     * @return A new <tt>MemoryIO</tt> instance that can access the memory.
     */
    static JNAMemoryIO wrap(Pointer ptr) {
        return ptr != null ? new PointerIO(ptr, 0) : NULL;
    }
    public abstract Pointer getAddress();
    public abstract Pointer getPointer(long offset);
    public abstract void putPointer(long offset, Pointer value);
    
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof JNAMemoryIO) && ((JNAMemoryIO) obj).memory.equals(memory);
    }

    @Override
    public int hashCode() {
        return memory.hashCode();
    }
    public JNAMemoryIO getMemoryIO(long offset) {
        return JNAMemoryIO.wrap(getPointer(offset));
    }
    public void putMemoryIO(long offset, MemoryIO value) {
        putPointer(offset, ((JNAMemoryIO) value).getAddress());
    }
    
    /**
     * A <tt>MemoryIO</tt> accessor that wraps a native pointer.
     */
    static final class PointerIO extends JNAMemoryIO {
        /**
         * The native pointer.
         */
        final Pointer ptr;

        /**
         * The size of the memory area.
         */
        final int size;

        /**
         * Allocates a new block of native memory and wraps it in a {@link MemoryIO}
         * accessor.
         * 
         * @param size The size in bytes of memory to allocate.
         * 
         * @return A new <tt>MemoryIO</tt> instance that can access the memory.
         */
        static JNAMemoryIO allocate(int size) {
            return new PointerIO(new Memory(size), size);
        }
        private PointerIO() {
            this(Pointer.NULL, 0);
        }
        private PointerIO(Pointer ptr, int size) {
            super(ptr);
            this.ptr = ptr;
            this.size = size;
        }
        public Pointer getAddress() {
            return ptr;
        }
        public boolean isNull() {
            return ptr == null;
        }
        public byte getByte(long offset) {
            return ptr.getByte(offset);
        }

        public short getShort(long offset) {
            return ptr.getShort(offset);
        }

        public int getInt(long offset) {
            return ptr.getInt(offset);
        }

        public long getLong(long offset) {
            return ptr.getLong(offset);
        }

        public long getNativeLong(long offset) {
            return ptr.getNativeLong(offset).longValue();
        }

        public float getFloat(long offset) {
            return ptr.getFloat(offset);
        }

        public double getDouble(long offset) {
            return ptr.getDouble(offset);
        }
        public Pointer getPointer(long offset) {
            return ptr.getPointer(offset);
        }
        
        public void putByte(long offset, byte value) {
            ptr.setByte(offset, value);
        }

        public void putShort(long offset, short value) {
            ptr.setShort(offset, value);
        }

        public void putInt(long offset, int value) {
            ptr.setInt(offset, value);
        }

        public void putLong(long offset, long value) {
            ptr.setLong(offset, value);
        }

        public void putNativeLong(long offset, long value) {
            ptr.setNativeLong(offset, new NativeLong(value));
        }

        public void putFloat(long offset, float value) {
            ptr.setFloat(offset, value);
        }

        public void putDouble(long offset, double value) {
            ptr.setDouble(offset, value);
        }
        
        public void putPointer(long offset, Pointer value) {
            ptr.setPointer(offset, value);
        }
        public void get(long offset, byte[] dst, int off, int len) {
            ptr.read(offset, dst, off, len);
        }

        public void put(long offset, byte[] dst, int off, int len) {
            ptr.write(offset, dst, off, len);
        }

        public void get(long offset, short[] dst, int off, int len) {
            ptr.read(offset, dst, off, len);
        }

        public void put(long offset, short[] dst, int off, int len) {
            ptr.write(offset, dst, off, len);
        }

        public void get(long offset, int[] dst, int off, int len) {
            ptr.read(offset, dst, off, len);
        }

        public void put(long offset, int[] dst, int off, int len) {
            ptr.write(offset, dst, off, len);
        }

        public void get(long offset, long[] dst, int off, int len) {
            ptr.read(offset, dst, off, len);
        }

        public void put(long offset, long[] dst, int off, int len) {
            ptr.write(offset, dst, off, len);
        }

        public void get(long offset, float[] dst, int off, int len) {
            ptr.read(offset, dst, off, len);
        }

        public void put(long offset, float[] dst, int off, int len) {
            ptr.write(offset, dst, off, len);
        }

        public void get(long offset, double[] dst, int off, int len) {
            ptr.read(offset, dst, off, len);
        }

        public void put(long offset, double[] dst, int off, int len) {
            ptr.write(offset, dst, off, len);
        }

        public int indexOf(long offset, byte value) {
            return (int) ptr.indexOf(offset, value);
        }

        public int indexOf(long offset, byte value, int maxlen) {
            return (int) ptr.indexOf(offset, value);
        }
        public void setMemory(long offset, long size, byte value) {
            ptr.setMemory(offset, size, value);
        }
        public void clear() {
            setMemory(0, size, (byte) 0);
        }
    }
    private static class BufferIO extends JNAMemoryIO {
        final ByteBuffer buffer;
        static JNAMemoryIO allocate(int size) {
            ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
            return new BufferIO(buf);
        }
        BufferIO(ByteBuffer buffer) {
            super(buffer);
            this.buffer = buffer;
        }
        public Pointer getAddress() {
            return Pointer.NULL;
        }
        public boolean isNull() {
            return false;
        }
        static ByteBuffer slice(ByteBuffer buffer, int position, int size) {
            ByteBuffer tmp = buffer.duplicate();
            tmp.position(position).limit(position + size);
            return tmp.slice();
        }
        @Override
        public Pointer getPointer(long offset) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void putPointer(long offset, Pointer value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public byte getByte(long offset) {
            return buffer.get((int) offset);
        }

        public short getShort(long offset) {
            return buffer.getShort((int) offset);
        }

        public int getInt(long offset) {
            return buffer.getInt((int) offset);
        }

        public long getLong(long offset) {
            return buffer.getLong((int) offset);
        }

        public long getNativeLong(long offset) {
            return NativeLong.SIZE == 4 
                    ? buffer.getInt((int) offset)
                    : buffer.getLong((int) offset);
        }

        public float getFloat(long offset) {
            return buffer.getFloat((int) offset);
        }

        public double getDouble(long offset) {
            return buffer.getDouble((int) offset);
        }

        public void putByte(long offset, byte value) {
            buffer.put((int) offset, value);
        }

        public void putShort(long offset, short value) {
            buffer.putShort((int) offset, value);
        }

        public void putInt(long offset, int value) {
            buffer.putInt((int) offset, value);
        }

        public void putLong(long offset, long value) {
            buffer.putLong((int) offset, value);
        }

        public void putNativeLong(long offset, long value) {
            if (NativeLong.SIZE == 4) {
                putInt(offset, (int) value);
            } else {
                putLong(offset, value);
            }
        }

        public void putFloat(long offset, float value) {
            buffer.putFloat((int) offset, value);
        }

        public void putDouble(long offset, double value) {
            buffer.putDouble((int) offset, value);
        }

        public void get(long offset, byte[] dst, int off, int len) {
            slice(buffer, (int) offset, len).get(dst, off, len);
        }

        public void put(long offset, byte[] dst, int off, int len) {
            slice(buffer, (int) offset, len).put(dst, off, len);
        }

        public void get(long offset, short[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 2).asShortBuffer().get(dst, off, len);
        }

        public void put(long offset, short[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 2).asShortBuffer().put(dst, off, len);
        }

        public void get(long offset, int[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 4).asIntBuffer().get(dst, off, len);
        }

        public void put(long offset, int[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 4).asIntBuffer().put(dst, off, len);
        }

        public void get(long offset, long[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 8).asLongBuffer().get(dst, off, len);
        }

        public void put(long offset, long[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 8).asLongBuffer().put(dst, off, len);
        }

        public void get(long offset, float[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 4).asFloatBuffer().get(dst, off, len);
        }

        public void put(long offset, float[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 4).asFloatBuffer().put(dst, off, len);
        }

        public void get(long offset, double[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 8).asDoubleBuffer().get(dst, off, len);
        }

        public void put(long offset, double[] dst, int off, int len) {
            slice(buffer, (int) offset, len * 8).asDoubleBuffer().put(dst, off, len);
        }

        public int indexOf(long offset, byte value) {
            return indexOf(offset, value, Integer.MAX_VALUE);
        }

        public int indexOf(long offset, byte value, int maxlen) {
            for (; offset > -1; ++offset) {
                if (buffer.get((int) offset) == value) {
                    return (int) offset;
                }
            }
            return -1;
        }
        public void setMemory(long offset, long size, byte value) {
            for (int i = 0; i < size; ++i) {
                buffer.put(i, value);
            }
        }
        public void clear() {
            setMemory(0, buffer.capacity(), (byte) 0);
        }
    }
}
