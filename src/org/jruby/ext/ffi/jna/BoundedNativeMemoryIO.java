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
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.jruby.Ruby;

/**
 * JNA implementation of memory I/O operations.
 */
public class BoundedNativeMemoryIO implements MemoryIO, DirectMemoryIO {
    private static final int ADDRESS_SIZE = Platform.getPlatform().addressSize();

    /* The runtime this memory I/O accessor is attached to */
    private final Ruby runtime;

    /** The native pointer. */
    protected final Pointer ptr;

    /** The size of the native memory area */
    protected final long size;

    public BoundedNativeMemoryIO(Ruby runtime, Pointer ptr, long size) {
        this.runtime = runtime;
        this.ptr = ptr;
        this.size = size;
    }

    private final void checkBounds(long off, long len) {
        Util.checkBounds(runtime, size, off, len);
    }

    Pointer getPointer() {
        return ptr;
    }

    public final java.nio.ByteBuffer asByteBuffer() {
        return ptr.getByteBuffer(0, size);
    }

    public final long getAddress() {
        PointerByReference ref = new PointerByReference(ptr);
        return ADDRESS_SIZE == 32
                ? (ref.getPointer().getInt(0) & 0xffffffffL) : ref.getPointer().getLong(0);
    }
    
    public final boolean isNull() {
        return ptr == null;
    }

    public final boolean isDirect() {
        return true;
    }

    public final byte getByte(long offset) {
        checkBounds(offset, 1);
        return ptr.getByte(offset);
    }

    public final short getShort(long offset) {
        checkBounds(offset, 2);
        return ptr.getShort(offset);
    }

    public final int getInt(long offset) {
        checkBounds(offset, 4);
        return ptr.getInt(offset);
    }

    public final long getLong(long offset) {
        checkBounds(offset, 8);
        return ptr.getLong(offset);
    }

    public final long getNativeLong(long offset) {
        checkBounds(offset, Platform.getPlatform().longSize() >> 3);
        return ptr.getNativeLong(offset).longValue();
    }

    public long getAddress(long offset) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        return ADDRESS_SIZE == 32
                ? (getInt(offset) & 0xffffffffL) : getLong(offset);
    }

    public final DirectMemoryIO getMemoryIO(long offset) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        return NativeMemoryIO.wrap(runtime, ptr.getPointer(offset));
    }

    public final float getFloat(long offset) {
        checkBounds(offset, 4);
        return ptr.getFloat(offset);
    }

    public final double getDouble(long offset) {
        checkBounds(offset, 8);
        return ptr.getDouble(offset);
    }

    public final Pointer getPointer(long offset) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        return ptr.getPointer(offset);
    }

    public final void putByte(long offset, byte value) {
        checkBounds(offset, 1);
        ptr.setByte(offset, value);
    }

    public final void putShort(long offset, short value) {
        checkBounds(offset, 2);
        ptr.setShort(offset, value);
    }

    public final void putInt(long offset, int value) {
        checkBounds(offset, 4);
        ptr.setInt(offset, value);
    }

    public final void putLong(long offset, long value) {
        checkBounds(offset, 8);
        ptr.setLong(offset, value);
    }

    public final void putNativeLong(long offset, long value) {
        checkBounds(offset, Platform.getPlatform().longSize() >> 3);
        ptr.setNativeLong(offset, new NativeLong(value));
    }

    public final void putAddress(long offset, long value) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        if (ADDRESS_SIZE == 32) {
            ptr.setInt(offset, (int) value);
        } else {
            ptr.setLong(offset, value);
        }
    }

    public final void putMemoryIO(long offset, MemoryIO value) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        if (value instanceof BoundedNativeMemoryIO) {
            ptr.setPointer(offset, ((BoundedNativeMemoryIO) value).ptr);
        } else {
            putAddress(offset, ((DirectMemoryIO) value).getAddress());
        }
    }

    public final void putFloat(long offset, float value) {
        checkBounds(offset, 4);
        ptr.setFloat(offset, value);
    }

    public final void putDouble(long offset, double value) {
        checkBounds(offset, 8);
        ptr.setDouble(offset, value);
    }

    public final void putPointer(long offset, Pointer value) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        ptr.setPointer(offset, value);
    }

    public final void get(long offset, byte[] dst, int off, int len) {
        checkBounds(offset, len);
        ptr.read(offset, dst, off, len);
    }

    public final void put(long offset, byte[] dst, int off, int len) {
        checkBounds(offset, len);
        ptr.write(offset, dst, off, len);
    }

    public final void get(long offset, short[] dst, int off, int len) {
        checkBounds(offset, len << 1);
        ptr.read(offset, dst, off, len);
    }

    public final void put(long offset, short[] dst, int off, int len) {
        checkBounds(offset, len << 1);
        ptr.write(offset, dst, off, len);
    }

    public final void get(long offset, int[] dst, int off, int len) {
        checkBounds(offset, len << 2);
        ptr.read(offset, dst, off, len);
    }

    public final void put(long offset, int[] dst, int off, int len) {
        checkBounds(offset, len << 2);
        ptr.write(offset, dst, off, len);
    }

    public final void get(long offset, long[] dst, int off, int len) {
        checkBounds(offset, len << 8);
        ptr.read(offset, dst, off, len);
    }

    public final void put(long offset, long[] dst, int off, int len) {
        checkBounds(offset, len << 8);
        ptr.write(offset, dst, off, len);
    }

    public final void get(long offset, float[] dst, int off, int len) {
        checkBounds(offset, len << 4);
        ptr.read(offset, dst, off, len);
    }

    public final void put(long offset, float[] dst, int off, int len) {
        checkBounds(offset, len << 4);
        ptr.write(offset, dst, off, len);
    }

    public final void get(long offset, double[] dst, int off, int len) {
        checkBounds(offset, len << 8);
        ptr.read(offset, dst, off, len);
    }

    public final void put(long offset, double[] dst, int off, int len) {
        checkBounds(offset, len << 8);
        ptr.write(offset, dst, off, len);
    }

    public final int indexOf(long offset, byte value) {
        return (int) ptr.indexOf(offset, value);
    }

    public final int indexOf(long offset, byte value, int maxlen) {
        return (int) ptr.indexOf(offset, value);
    }

    public final void setMemory(long offset, long size, byte value) {
        checkBounds(offset, size);
        ptr.setMemory(offset, size, value);
    }
    public byte[] getZeroTerminatedByteArray(long offset) {
        checkBounds(offset, 1);
        return ptr.getByteArray(offset, (int) ptr.indexOf(offset, (byte) 0));
    }

    public byte[] getZeroTerminatedByteArray(long offset, int maxlen) {
        checkBounds(offset, 1);
        return ptr.getByteArray(offset, (int) ptr.indexOf(offset, (byte) 0));
    }

    public void putZeroTerminatedByteArray(long offset, byte[] bytes, int off, int len) {
        // Ensure room for terminating zero byte
        checkBounds(offset, len + 1);
        ptr.write(offset, bytes, off, len);
        ptr.setByte(offset + len, (byte) 0);
    }

    public BoundedNativeMemoryIO slice(long offset) {
        checkBounds(offset, 1);
        return offset == 0 ? this : new BoundedNativeMemoryIO(runtime, ptr.share(offset), size - offset);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof BoundedNativeMemoryIO) {
            BoundedNativeMemoryIO io = (BoundedNativeMemoryIO) obj;
            return io.ptr == ptr || (io.ptr != null && io.ptr.equals(ptr));
        }
        
        return (obj instanceof DirectMemoryIO) && ((DirectMemoryIO) obj).getAddress() == getAddress();
    }

    @Override
    public int hashCode() {
        return ptr == null ? 0 : ptr.hashCode();
    }
}
