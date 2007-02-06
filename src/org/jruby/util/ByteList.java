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
    private static final double FACTOR = 1.5;
    
    /** Creates a new instance of ByteList */
    public ByteList() {
        this(DEFAULT_SIZE);
    }
    
    public ByteList(int size) {
        bytes = new byte[size];
        realSize = 0;
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
    
    public int size() {
        return realSize;
    }
    
    public byte[] bytes() {
        byte[] newBytes = new byte[realSize];
        System.arraycopy(bytes, 0, newBytes, 0, realSize);
        return newBytes;
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
