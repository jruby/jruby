/*
 * ByteList.java
 *
 * Created on February 5, 2007, 10:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.util;

import java.util.Arrays;

/**
 *
 * @author headius
 */
public class ByteList {
    public static final byte[] NULL_ARRAY = new byte[0];
    
    private byte[] bytes;
    private int realSize;
    
    private static final int DEFAULT_SIZE = 4;
    private static final double FACTOR = 2;
    
    /** Creates a new instance of ByteList */
    public ByteList() {
        this(DEFAULT_SIZE);
    }
    
    public ByteList(int size) {
        bytes = new byte[size];
        realSize = 0;
    }
    
    public ByteList(byte[] wrap) {
        if (wrap == null) throw new RuntimeException("Invalid argument: constructing with null array");
        bytes = (byte[])wrap.clone();
        realSize = wrap.length;
    }
    
    public ByteList(ByteList wrap) {
        this(wrap.bytes, 0, wrap.realSize);
    }
    
    public ByteList(byte[] wrap, int index, int len) {
        if (wrap == null) throw new RuntimeException("Invalid argument: constructing with null array");
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
            System.arraycopy(bytes, 0, bytes, 1, bytes.length);
        }
        bytes[0] = b;
        realSize++;
    }
    
    public void prepend(int b) {
        prepend((byte)b);
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
    
    public void prepend(byte[] moreBytes) {
        if (realSize + moreBytes.length <= bytes.length) {
            System.arraycopy(bytes, 0, bytes, moreBytes.length, realSize);
            System.arraycopy(moreBytes, 0, bytes, 0, moreBytes.length);
        } else {
            byte[] newBytes = new byte[(int)((realSize + moreBytes.length) * FACTOR)];
            System.arraycopy(moreBytes, 0, newBytes, 0, moreBytes.length);
            System.arraycopy(bytes, 0, newBytes, moreBytes.length, realSize);
            bytes = newBytes;
        }
        realSize += moreBytes.length;
    }
    
    public int unget() { 
        return --realSize;
    }
    
    public int length() {
        return realSize;
    }
    
    public void length(int newLength) {
        // TODO: check valid, grow if needed?
        realSize = newLength;
    }
    
    public int get(int index) {
        // TODO: bounds checking? or assume good for speed?
        return bytes[index];
    }
    
    public void set(int index, int b) {
        // TODO: bounds checking? or assume good for speed?
        bytes[index] = (byte)b;
    }
    
    public void replace(byte[] newBytes) {
        if (newBytes == null) throw new RuntimeException("Invalid argument: replacing with null array");
        this.bytes = newBytes;
        realSize = newBytes.length;
    }
    
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof ByteList) {
            // could be more efficient
            return Arrays.equals(this.bytes(), ((ByteList)other).bytes());
        }
        return false;
    }
    
    public byte[] bytes() {
        byte[] newBytes = new byte[realSize];
        System.arraycopy(bytes, 0, newBytes, 0, realSize);
        return newBytes;
    }
    
    public Object clone() {
        return new ByteList(bytes());
    }
    
    public static byte[] append(byte[] bytes, int b) {
        byte[] newBytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        newBytes[bytes.length] = (byte)b;
        
        return newBytes;
    }
    
    public static byte[] prepend(byte[] bytes, int b) {
        byte[] newBytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 1, newBytes, 0, bytes.length);
        newBytes[0] = (byte)b;
        
        return newBytes;
    }
    
}
