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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jruby.util;

import java.io.Serializable;


/**
 *
 * @author headius
 */
public class ByteList implements Comparable, CharSequence, Serializable {
    private static final long serialVersionUID = -1286166947275543731L;

    public static final byte[] NULL_ARRAY = new byte[0];

    public byte[] bytes;
    public int realSize;

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

    public ByteList(byte[] wrap) {
        this(wrap,true);
    }

    public ByteList(byte[] wrap, boolean copy) {
        if (wrap == null) throw new NullPointerException("Invalid argument: constructing with null array");
        if(copy) {
            bytes = (byte[])wrap.clone();
        } else {
            bytes = wrap;
        }
        realSize = wrap.length;
    }

    public ByteList(ByteList wrap) {
        this(wrap.bytes, 0, wrap.realSize);
    }

    public ByteList(byte[] wrap, int index, int len) {
        this(wrap,index,len,true);
    }

    public ByteList(byte[] wrap, int index, int len, boolean copy) {
        if (wrap == null) throw new NullPointerException("Invalid argument: constructing with null array");
        if(copy || index != 0) {
            bytes = new byte[len];
            System.arraycopy(wrap, index, bytes, 0, len);
        } else {
            bytes = wrap;
        }
        realSize = len;
    }

    public ByteList(ByteList wrap, int index, int len) {
        this(wrap.bytes, index, len);
    }

    public void delete(int start, int len) {
        realSize-=len;
        System.arraycopy(bytes,start+len,bytes,start,realSize);
    }

    public void append(byte b) {
        grow(1);
        bytes[realSize++] = b;
    }

    public void append(int b) {
        append((byte)b);
    }

    public void prepend(byte b) {
        grow(1);
        System.arraycopy(bytes, 0, bytes, 1, realSize);
        bytes[0] = b;
        realSize++;
    }

    public void append(byte[] moreBytes) {
        grow(moreBytes.length);
        System.arraycopy(moreBytes, 0, bytes, realSize, moreBytes.length);
        realSize += moreBytes.length;
    }

    public void append(ByteList moreBytes) {
        append(moreBytes.bytes, 0, moreBytes.realSize);
    }

    public void append(ByteList moreBytes, int index, int len) {
        append(moreBytes.bytes, index, len);
    }

    public void append(byte[] moreBytes, int start, int len) {
        grow(len);
        System.arraycopy(moreBytes, start, bytes, realSize, len);
        realSize += len;
    }

    public int length() {
        return realSize;
    }

    public void length(int newLength) {
        grow(newLength - realSize);
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

    /**
     * Unsafe version of replace(int,int,ByteList). The contract is that these
     * unsafe versions will not make sure thet beg and len indices are correct.
     */
    public void unsafeReplace(int beg, int len, ByteList nbytes) {
        unsafeReplace(beg, len, nbytes.bytes, 0, nbytes.realSize);
    }

    /**
     * Unsafe version of replace(int,int,byte[]). The contract is that these
     * unsafe versions will not make sure thet beg and len indices are correct.
     */
    public void unsafeReplace(int beg, int len, byte[] buf) {
        unsafeReplace(beg, len, buf, 0, buf.length);
    }

    /**
     * Unsafe version of replace(int,int,byte[],int,int). The contract is that these
     * unsafe versions will not make sure thet beg and len indices are correct.
     */
    public void unsafeReplace(int beg, int len, byte[] nbytes, int index, int count) {
        grow(count - len);
        int newSize = realSize + count - len;
        System.arraycopy(bytes,beg+len,bytes,beg+count,realSize - (len+beg));
        System.arraycopy(nbytes,index,bytes,beg,count);
        realSize = newSize;
    }

    public void replace(int beg, int len, ByteList nbytes) {
        replace(beg, len, nbytes.bytes, 0, nbytes.realSize);
    }

    public void replace(int beg, int len, byte[] buf) {
        replace(beg, len, buf, 0, buf.length);
    }

    public void replace(int beg, int len, byte[] nbytes, int index, int count) {
        if (len - beg > realSize) throw new IndexOutOfBoundsException();
        unsafeReplace(beg,len,nbytes,index,count);
    }

    public void insert(int index, int b) {
        if (index >= realSize) throw new IndexOutOfBoundsException();
        grow(1);
        System.arraycopy(bytes,index,bytes,index+1,realSize-index);
        bytes[index] = (byte)b;
        realSize++;
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

    /**
     * This comparison matches MRI comparison of Strings (rb_str_cmp).
     * I wish we had memcmp right now...
     */
    public int compareTo(Object other) {
        return cmp((ByteList)other);
    }

    public int cmp(ByteList other) {
        if (other == this || other.bytes == bytes) return 0;
        int len = Math.min(realSize,other.realSize);
        int retval = 0;
        for(int i=0;i<len && retval == 0;i++) {
            retval = (bytes[i]&0xFF) - (other.bytes[i]&0xFF);
        }
        if(retval == 0) {
            if(realSize == other.realSize) {
                return 0;
            } else if(realSize > len) {
                return 1;
            }
            return -1;
        }
        if(retval > 0) {
            return 1;
        }
        return -1;
    }

    /**
     * Returns the internal byte array. This is unsafe unless you know what you're
     * doing. But it can improve performance for byte-array operations that
     * won't change the array.
     *
     * @return the internal byte array
     */
    public byte[] unsafeBytes() {
        return bytes;
    }

    public byte[] bytes() {
        byte[] newBytes = new byte[realSize];
        System.arraycopy(bytes, 0, newBytes, 0, realSize);
        return newBytes;
    }

    public Object clone() {
        return new ByteList(bytes, 0, realSize);
    }

    private void grow(int increaseRequested) {
        if (increaseRequested < 0) {
            return;
        }
        int newSize = realSize + increaseRequested;
        if (bytes.length < newSize) {
            byte[] newBytes = new byte[(int) (newSize * FACTOR)];
            System.arraycopy(bytes,0,newBytes,0,realSize);
            bytes = newBytes;
        }
    }

    /**
     * Implements the same hashcode as MRI ELFHASH in rb_str_hash.
     * No caching here. It's the clients responsibility to cache this value.
     */
    public int hashCode() {
        int key = 0;
        int g;
        for(int i = 0; i < realSize; i++) {
            key = (key << 4) + (((int)bytes[i]) & 0xFF);
            if((g = key & 0xF0000000) != 0) {
                key ^= g >> 24;
            }
            key &= ~g;
        }
        return key;
    }

    public String toString() {
        return new String(this.bytes,0,realSize);
    }

    public static ByteList create(CharSequence s) {
        return new ByteList(plain(s),false);
    }

    public static byte[] plain(CharSequence s) {
        byte[] bytes = new byte[s.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) s.charAt(i);
        }
        return bytes;
    }

    public static byte[] plain(char[] s) {
        byte[] bytes = new byte[s.length];
        for (int i = 0; i < s.length; i++) {
            bytes[i] = (byte) s[i];
        }
        return bytes;
    }
    
    public static char[] plain(byte[] b) {
        char[] chars = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            chars[i] = (char) (b[i] & 0xFF);
        }
        return chars;
    }

    public char charAt(int ix) {
        return (char)(this.bytes[ix] & 0xFF);
    }

    public CharSequence subSequence(int start, int end) {
        return new ByteList(this, start, end-start);
    }
}
