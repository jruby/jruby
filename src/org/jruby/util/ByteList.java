/*
 * ByteList.java
 *
 * Created on February 5, 2007, 10:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.util;


/**
 *
 * @author headius
 */
public class ByteList {
    public static final byte[] NULL_ARRAY = new byte[0];

    private byte[] bytes;
    private int realSize;

    private static final int DEFAULT_SIZE = 4;
    private static final double FACTOR = 2.0;

    /** Creates a new instance of ByteList */
    public ByteList() {
        this(DEFAULT_SIZE);
    }

    public ByteList(int size) {
        bytes = new byte[size];
        realSize = 0;
    }

    public ByteList(byte[] wrap) {
        if (wrap == null) throw new NullPointerException("Invalid argument: constructing with null array");
        bytes = (byte[])wrap.clone();
        realSize = wrap.length;
    }

    public ByteList(ByteList wrap) {
        this(wrap.bytes, 0, wrap.realSize);
    }

    public ByteList(byte[] wrap, int index, int len) {
        if (wrap == null) throw new NullPointerException("Invalid argument: constructing with null array");
        bytes = new byte[len];
        System.arraycopy(wrap, index, bytes, 0, len);
        realSize = len;
    }

    public ByteList(ByteList wrap, int index, int len) {
        this(wrap.bytes, index, len);
    }

    public void append(byte b) {
        if (realSize == bytes.length) {
            byte[] newBytes = new byte[(int)(bytes.length * FACTOR)];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
            bytes = newBytes;
        }
        bytes[realSize++] = b;
    }

    public void append(int b) {
        append((byte)b);
    }

    public void prepend(byte b) {
        if (realSize == bytes.length) {
            byte[] newBytes = new byte[(int)(bytes.length * FACTOR)];
            System.arraycopy(bytes, 0, newBytes, 1, bytes.length);
            bytes = newBytes;
        } else {
            for (int i = realSize; i > 0; i--) {
                bytes[i] = bytes[i-1];
            }
        }
        bytes[0] = b;
        realSize++;
    }

    public void append(byte[] moreBytes) {
        if (realSize + moreBytes.length <= bytes.length) {
            System.arraycopy(moreBytes, 0, bytes, realSize, moreBytes.length);
        } else {
            byte[] newBytes = new byte[(int)((realSize + moreBytes.length) * FACTOR)];
            System.arraycopy(bytes, 0, newBytes, 0, realSize);
            System.arraycopy(moreBytes, 0, newBytes, realSize, moreBytes.length);
            bytes = newBytes;
        }
        realSize += moreBytes.length;
    }

    public void append(ByteList moreBytes) {
        append(moreBytes.bytes, 0, moreBytes.realSize);
    }

    public void append(ByteList moreBytes, int index, int len) {
        append(moreBytes.bytes, index, len);
    }

    public void append(byte[] moreBytes, int start, int len) {
        if (realSize + len <= bytes.length) {
            System.arraycopy(moreBytes, start, bytes, realSize, len);
        } else {
            byte[] newBytes = new byte[(int)((realSize + len) * FACTOR)];
            System.arraycopy(bytes, 0, newBytes, 0, realSize);
            System.arraycopy(moreBytes, start, newBytes, realSize, len);
            bytes = newBytes;
        }
        realSize += len;
    }

    public int length() {
        return realSize;
    }

    public void length(int newLength) {
        if (bytes.length < newLength) {
            byte[] newBytes = new byte[(int) (newLength * FACTOR)];
            System.arraycopy(bytes, 0, newBytes, 0, realSize);
            bytes = newBytes;
        }
        realSize = newLength;
    }

    public int get(int index) {
        if (index >= realSize) throw new IndexOutOfBoundsException();
        return bytes[index];
    }

    public void set(int index, int b) {
        if (index >= realSize) throw new IndexOutOfBoundsException();
        bytes[index] = (byte)b;
    }

    public void replace(byte[] newBytes) {
        if (newBytes == null) throw new NullPointerException("Invalid argument: replacing with null array");
        this.bytes = newBytes;
        realSize = newBytes.length;
    }

    public void replace(int beg, int len, ByteList nbytes) {
        replace(beg, len, nbytes.bytes, 0, nbytes.realSize);
    }

    public void replace(int beg, int len, byte[] buf) {
        replace(beg, len, buf, 0, buf.length);
    }

    public void replace(int beg, int len, byte[] nbytes, int index, int count) {
        if (len - beg > realSize) throw new IndexOutOfBoundsException();
        int newSize = realSize - len + count;
        byte[] newBytes = bytes;
        if (newSize > bytes.length) {
            newBytes = new byte[(int) (newSize * FACTOR)];
            System.arraycopy(bytes,0,newBytes,0,beg);
        }
        System.arraycopy(bytes,beg+len,newBytes,beg+count,realSize - (len+beg));
        System.arraycopy(nbytes,index,newBytes,beg,count);
        bytes = newBytes;
        realSize = newSize;
    }

    public int hashCode() {
        return bytes.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof ByteList) {
            ByteList b = (ByteList) other;
            if (b.realSize != realSize) {
                return false;
            }
            for (int i = 0; i < realSize; i++) {
                if (bytes[i] != b.bytes[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public byte[] bytes() {
        byte[] newBytes = new byte[realSize];
        System.arraycopy(bytes, 0, newBytes, 0, realSize);
        return newBytes;
    }

    public Object clone() {
        return new ByteList(bytes, 0, realSize);
    }
}
