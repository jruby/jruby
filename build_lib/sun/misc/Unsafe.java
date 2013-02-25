/*
 * **** BEGIN LICENSE BLOCK ***** Version: EPL 1.0/GPL 2.0/LGPL 2.1 The contents
 * of this file are subject to the Eclipse Public License Version 1.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.eclipse.org/legal/cpl-v10.html Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing rights and limitations under the License. Alternatively, the
 * contents of this file may be used under the terms of either of the GNU
 * General Public License Version 2 or later (the "GPL"), or the GNU Lesser
 * General Public License Version 2.1 or later (the "LGPL"), in which case the
 * provisions of the GPL or the LGPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * either the GPL or the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting the
 * provisions above and replace them with the notice and other provisions
 * required by the GPL or the LGPL. If you do not delete the provisions above, a
 * recipient may use your version of this file under the terms of any one of the
 * CPL, the GPL or the LGPL.**** END LICENSE BLOCK ****
 */
package sun.misc;

import java.lang.reflect.Field;
import java.security.ProtectionDomain;

public abstract class Unsafe {
    
    private static Unsafe theUnsafe;
    public abstract int getInt(Object o, long offset);
    public abstract void putInt(Object o, long offset, int x);
    public abstract Object getObject(Object o, long offset);
    public abstract void putObject(Object o, long offset, Object x);
    public abstract boolean getBoolean(Object o, long offset);
    public abstract void putBoolean(Object o, long offset, boolean x);
    public abstract byte getByte(Object o, long offset);
    public abstract void putByte(Object o, long offset, byte x);
    public abstract short getShort(Object o, long offset);
    public abstract void putShort(Object o, long offset, short x);
    public abstract char getChar(Object o, long offset);
    public abstract void putChar(Object o, long offset, char x);
    public abstract long getLong(Object o, long offset);
    public abstract void putLong(Object o, long offset, long x);
    public abstract float getFloat(Object o, long offset);
    public abstract void putFloat(Object o, long offset, float x);
    public abstract double getDouble(Object o, long offset);
    public abstract void putDouble(Object o, long offset, double x);
    @Deprecated
    public abstract int getInt(Object o, int offset);
    @Deprecated
    public abstract void putInt(Object o, int offset, int x);
    @Deprecated
    public abstract Object getObject(Object o, int offset);
    @Deprecated
    public abstract void putObject(Object o, int offset, Object x);
    @Deprecated
    public abstract boolean getBoolean(Object o, int offset);
    @Deprecated
    public abstract void putBoolean(Object o, int offset, boolean x);
    @Deprecated
    public abstract byte getByte(Object o, int offset);
    @Deprecated
    public abstract void putByte(Object o, int offset, byte x);
    @Deprecated
    public abstract short getShort(Object o, int offset);
    @Deprecated
    public abstract void putShort(Object o, int offset, short x);
    @Deprecated
    public abstract char getChar(Object o, int offset);
    @Deprecated
    public abstract void putChar(Object o, int offset, char x);
    @Deprecated
    public abstract long getLong(Object o, int offset);
    @Deprecated
    public abstract void putLong(Object o, int offset, long x);
    @Deprecated
    public abstract float getFloat(Object o, int offset);
    @Deprecated
    public abstract void putFloat(Object o, int offset, float x);
    @Deprecated
    public abstract double getDouble(Object o, int offset);
    @Deprecated
    public abstract void putDouble(Object o, int offset, double x);
    public abstract byte getByte(long address);
    public abstract void putByte(long address, byte x);
    public abstract short getShort(long address);
    public abstract void putShort(long address, short x);
    public abstract char getChar(long address);
    public abstract void putChar(long address, char x);
    public abstract int getInt(long address);
    public abstract void putInt(long address, int x);
    public abstract long getLong(long address);
    public abstract void putLong(long address, long x);
    public abstract float getFloat(long address);
    public abstract void putFloat(long address, float x);
    public abstract double getDouble(long address);
    public abstract void putDouble(long address, double x);
    public abstract long getAddress(long address);
    public abstract void putAddress(long address, long x);
    public abstract long allocateMemory(long bytes);
    public abstract long reallocateMemory(long address, long bytes);
    public abstract void setMemory(Object o, long offset, long bytes, byte value);
    public abstract void setMemory(long address, long bytes, byte value);
    public abstract void copyMemory(Object srcBase, long srcOffset, Object destBase,
            long destOffset, long bytes);
    public abstract void copyMemory(long srcAddress, long destAddress, long bytes);

    public abstract void freeMemory(long address);
    public static final int INVALID_FIELD_OFFSET = 0;
    @Deprecated
    public abstract int fieldOffset(Field f);
    @Deprecated
    public abstract Object staticFieldBase(Class<?> c);
    public abstract long staticFieldOffset(Field f);
    public abstract long objectFieldOffset(Field f);
    public abstract Object staticFieldBase(Field f);
    public abstract boolean shouldBeInitialized(Class<?> c);
    public abstract void ensureClassInitialized(Class<?> c);
    public abstract int arrayBaseOffset(Class<?> arrayClass);

    public static final int ARRAY_BOOLEAN_BASE_OFFSET = 0;
    public static final int ARRAY_BYTE_BASE_OFFSET = 0;
    public static final int ARRAY_SHORT_BASE_OFFSET = 0;
    public static final int ARRAY_CHAR_BASE_OFFSET = 0;
    public static final int ARRAY_INT_BASE_OFFSET = 0;
    public static final int ARRAY_LONG_BASE_OFFSET = 0;
    public static final int ARRAY_FLOAT_BASE_OFFSET = 0;
    public static final int ARRAY_DOUBLE_BASE_OFFSET = 0;
    public static final int ARRAY_OBJECT_BASE_OFFSET = 0;
    public abstract int arrayIndexScale(Class<?> arrayClass);

    public static final int ARRAY_BOOLEAN_INDEX_SCALE = 0;
    public static final int ARRAY_BYTE_INDEX_SCALE = 0;
    public static final int ARRAY_SHORT_INDEX_SCALE = 0;
    public static final int ARRAY_CHAR_INDEX_SCALE = 0;
    public static final int ARRAY_INT_INDEX_SCALE = 0;
    public static final int ARRAY_LONG_INDEX_SCALE = 0;
    public static final int ARRAY_FLOAT_INDEX_SCALE = 0;
    public static final int ARRAY_DOUBLE_INDEX_SCALE = 0;
    public static final int ARRAY_OBJECT_INDEX_SCALE = 0;
    public abstract int addressSize();
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();
    public abstract int pageSize();
    public abstract Class<?> defineClass(String name, byte[] b, int off, int len,
            ClassLoader loader, ProtectionDomain protectionDomain);
    public abstract Class<?> defineClass(String name, byte[] b, int off, int len);
    public abstract Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data,
            Object[] cpPatches);
    public abstract Object allocateInstance(Class<?> cls) throws InstantiationException;
    public abstract void monitorEnter(Object o);
    public abstract void monitorExit(Object o);
    public abstract boolean tryMonitorEnter(Object o);
    public abstract void throwException(Throwable ee);
    public abstract boolean compareAndSwapObject(Object o, long offset, Object expected, Object x);
    public abstract boolean compareAndSwapInt(Object o, long offset, int expected, int x);
    public abstract boolean compareAndSwapLong(Object o, long offset, long expected, long x);
    public abstract Object getObjectVolatile(Object o, long offset);
    public abstract void putObjectVolatile(Object o, long offset, Object x);
    public abstract int getIntVolatile(Object o, long offset);
    public abstract void putIntVolatile(Object o, long offset, int x);
    public abstract boolean getBooleanVolatile(Object o, long offset);
    public abstract void putBooleanVolatile(Object o, long offset, boolean x);
    public abstract byte getByteVolatile(Object o, long offset);
    public abstract void putByteVolatile(Object o, long offset, byte x);
    public abstract short getShortVolatile(Object o, long offset);
    public abstract void putShortVolatile(Object o, long offset, short x);
    public abstract char getCharVolatile(Object o, long offset);
    public abstract void putCharVolatile(Object o, long offset, char x);
    public abstract long getLongVolatile(Object o, long offset);
    public abstract void putLongVolatile(Object o, long offset, long x);
    public abstract float getFloatVolatile(Object o, long offset);
    public abstract void putFloatVolatile(Object o, long offset, float x);
    public abstract double getDoubleVolatile(Object o, long offset);
    public abstract void putDoubleVolatile(Object o, long offset, double x);
    public abstract void putOrderedObject(Object o, long offset, Object x);
    public abstract void putOrderedInt(Object o, long offset, int x);
    public abstract void putOrderedLong(Object o, long offset, long x);
    public abstract void unpark(Object thread);
    public abstract void park(boolean isAbsolute, long time);
    public abstract int getLoadAverage(double[] loadavg, int nelems);
    public abstract int getAndAddInt(Object o, long offset, int delta);
    public abstract long getAndAddLong(Object o, long offset, long delta);
    public abstract int getAndSetInt(Object o, long offset, int newValue);
    public abstract long getAndSetLong(Object o, long offset, long newValue);
    public abstract Object getAndSetObject(Object o, long offset, Object newValue);
    public abstract void loadFence();
    public abstract void storeFence();
    public abstract void fullFence();

}
