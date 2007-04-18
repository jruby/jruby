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
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
public final class ByteList implements Comparable, CharSequence, Serializable {
    private static final long serialVersionUID = -1286166947275543731L;

    public static final byte[] NULL_ARRAY = new byte[0];

    public byte[] bytes;
    public int begin;
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
        this(wrap.bytes, wrap.begin, wrap.realSize);
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
    
    private ByteList(boolean flag) {
        
    }

    public void delete(int start, int len) {
        realSize-=len;
        System.arraycopy(bytes,start+len,bytes,start,realSize);
    }

    public ByteList append(byte b) {
        grow(1);
        bytes[realSize++] = b;
        return this;
    }

    public ByteList append(int b) {
        append((byte)b);
        return this;
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
        append(moreBytes.bytes, moreBytes.begin, moreBytes.realSize);
    }

    public void append(ByteList moreBytes, int index, int len) {
        append(moreBytes.bytes, moreBytes.begin + index, len);
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
        return bytes[begin + index];
    }

    public void set(int index, int b) {
        if (index >= realSize) throw new IndexOutOfBoundsException();
        bytes[begin + index] = (byte)b;
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
    
    public int indexOf(int c) {
        return indexOf(c, 0);
    }
    
    // leaving old version for now for comparisons, see 
    // TestByteList.java attached to JRUBY-xxx
    public int old_indexOf(int c, int pos) {
        final int end = begin + realSize;
        pos += begin;
        for (int i = pos; i < end; i++) {
            if ((bytes[i] & 0xFF) == c) return i - begin;
        }
        return -1;
    }
    
    public int indexOf(final int c, int pos) {
        // not sure if this is checked elsewhere,
        // didn't see it in RubyString. RubyString does
        // cast to char, so c will be >= 0.
        if (c > 255)
            return -1;
        final byte b = (byte)(c&0xFF);
        final int size = begin + realSize;
        final byte[] buf = bytes;
        pos += begin;
        for ( ; pos < size && buf[pos] != b ; pos++ ) ;
        return pos < size ? pos - begin : -1;
    }

    public int indexOf(ByteList find) {
        return indexOf(find, 0);
    }
    
    // leaving old version for now for comparisons, see 
    // TestByteList.java attached to JRUBY-xxx
    public int old_indexOf(ByteList find, int pos) {
        // search main string
        BigLoop: for (int j = begin + pos; j < begin + realSize; j++) {
            // if we get a match for the first char
            if ((bytes[j] & 0xFF) == find.bytes[0]) {
                // try to match the remaining chars
                for (int k = begin + 1; k < begin + find.realSize; k++) {
                    // reached end of search string, break search loop
                    if (j + k >= begin + realSize) break BigLoop;
                    
                    //some character didn't match, back to main loop with next char
                    if (bytes[j + k] != find.bytes[k]) continue BigLoop;
                }
                // got through find string successfully, return j
                return j - begin;
            }
        }
        return -1;
    }
    
    public int indexOf(final ByteList find, int pos) {
        final int len = find.realSize;
        if (len == 0) return -1;

        final byte first = find.bytes[find.begin];
        final byte[] buf = bytes;
        final int max = realSize - len + 1;
        for ( ; pos < max ; pos++ ) {
            for ( ; pos < max && buf[begin + pos] != first; pos++ ) ;
            if (pos == max)
                return -1;
            int index = len;
            // TODO: forward/backward scan as in #equals
            for ( ; --index >= 0 && buf[begin + index + pos] == find.bytes[find.begin + index]; ) ;
            if (index < 0)
                return pos;
        }
        return -1;
    }

    public int lastIndexOf(int c) {
        return lastIndexOf(c, realSize - 1);
    }
    
    public int old_lastIndexOf(int c, int pos) {
        for (int i = Math.min(pos, realSize - 1); i >= 0; i--) {
            if ((bytes[begin + i] & 0xFF) == c) return i;
        }
        return -1;
    }

    // leaving old version for now for comparisons, see 
    // TestByteList.java attached to JRUBY-xxx    
    public int lastIndexOf(final int c, int pos) {
        // not sure if this is checked elsewhere,
        // didn't see it in RubyString. RubyString does
        // cast to char, so c will be >= 0.
        if (c > 255)
            return -1;
        final byte b = (byte)(c&0xFF);
        final int size = begin + realSize;
        pos += begin;
        final byte[] buf = bytes;
        if (pos >= size) {
            pos = size;
        } else {
            pos++;
        }
        for ( ; --pos >= begin && buf[pos] != b ; ) ;
        return pos - begin;
    }

    public int lastIndexOf(ByteList find) {
        return lastIndexOf(find, realSize - 1);
    }
    
    // leaving old version for now for comparisons, see 
    // TestByteList.java attached to JRUBY-xxx
    public int old_lastIndexOf(ByteList find, int pos) {
        // search main string
        BigLoop: for (int j = Math.min(pos, realSize - 1); j >= 0; j--) {
            // if we get a match for the first char
            if ((bytes[begin + j] & 0xFF) == find.bytes[find.begin]) {
                // try to match the remaining chars
                for (int k = 1; k < find.realSize; k++) {
                    // reached end of search string, continue search loop at previous char
                    if (j + k >= realSize) continue BigLoop;

                    //some character didn't match, back to main loop with next char
                    if (bytes[begin + j + k] != find.bytes[find.begin + k]) continue BigLoop;
                }
                // got through find string successfully, return j
                return j;
            }
        }

        return -1;
    }

    public int lastIndexOf(final ByteList find, int pos) {
        final int len = find.realSize;
        if (len == 0) return -1;

        final byte first = find.bytes[find.begin];
        final byte[] buf = bytes;
        pos = Math.min(pos,realSize-len);
        for ( ; pos >= 0 ; pos-- ) {
            for ( ; pos >= 0 && buf[begin + pos] != first; pos-- ) ;
            if (pos < 0)
                return -1;
            int index = len;
            // TODO: forward/backward scan as in #equals
            for ( ; --index >= 0 && buf[begin + index + pos] == find.bytes[find.begin + index]; ) ;
            if (index < 0)
                return pos;
        }
        return -1;
    }
    
    // leaving old version for now for comparisons, see 
    // TestByteList.java attached to JRUBY-xxx
    public boolean old_equals(Object other) {
        if (other == this) return true;
        if (other instanceof ByteList) {
            ByteList b = (ByteList) other;
            if (b.realSize != realSize) {
                return false;
            }
            for (int i = 0; i < realSize; i++) {
                if (bytes[begin + i] != b.bytes[b.begin + i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof ByteList) {
            ByteList b = (ByteList) other;
            int first;
            int last;
            byte[] buf;
            if ((last = realSize) == b.realSize) {
                // scanning from front and back simultaneously, meeting in
                // the middle. the object is to get a mismatch as quickly as
                // possible. alternatives might be: scan from the middle outward
                // (not great because it won't pick up common variations at the
                // ends until late) or sample odd bytes forward and even bytes
                // backward (I like this one, but it's more expensive for
                // strings that are equal; see sample_equals below).
                for (buf = bytes, first = -1; 
                    --last > first && buf[begin + last] == b.bytes[b.begin + last] &&
                    ++first < last && buf[begin + first] == b.bytes[b.begin + first] ; ) ;
                return first >= last;
            }
        }
        return false;
    }

    // an alternative to the new version of equals, should
    // detect inequality faster (in many cases), but is slow
    // in the case of equal values (all bytes visited), due to
    // using n+=2, n-=2 vs. ++n, --n while iterating over the array.
    public boolean sample_equals(Object other) {
        if (other == this) return true;
        if (other instanceof ByteList) {
            ByteList b = (ByteList) other;
            int first;
            int last;
            int size;
            byte[] buf;
            if ((size = realSize) == b.realSize) {
                // scanning from front and back simultaneously, sampling odd
                // bytes on the forward iteration and even bytes on the 
                // reverse iteration. the object is to get a mismatch as quickly
                // as possible. 
                for (buf = bytes, first = -1, last = (size + 1) & ~1 ;
                    (last -= 2) >= 0 && buf[begin + last] == b.bytes[b.begin + last] &&
                    (first += 2) < size && buf[begin + first] == b.bytes[b.begin + first] ; ) ;
                return last < 0 || first == size;
            }
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

    // leaving old version for now for comparisons, see 
    // TestByteList.java attached to JRUBY-xxx
    public int old_cmp(ByteList other) {
        if (other == this || other.bytes == bytes) return 0;
        int len = Math.min(realSize,other.realSize);
        int retval = 0;
        for(int i=0;i<len && retval == 0;i++) {
            retval = (bytes[begin + i]&0xFF) - (other.bytes[other.begin + i]&0xFF);
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

    public int cmp(final ByteList other) {
        if (other == this || bytes == other.bytes) return 0;
        final int size = realSize;
        final int len =  Math.min(size,other.realSize);
        int offset = -1;
        // a bit of VM/JIT weirdness here: though in most cases
        // performance is improved if array references are kept in
        // a local variable (saves an instruction per access, as I
        // [slightly] understand it), in some cases, when two (or more?) 
        // arrays are being accessed, the member reference is actually
        // faster.  this is one of those cases...
        for (  ; ++offset < len && bytes[begin + offset] == other.bytes[other.begin + offset]; ) ;
        if (offset < len) {
            return (bytes[begin + offset]&0xFF) > (other.bytes[other.begin + offset]&0xFF) ? 1 : -1;
        }
        return size == other.realSize ? 0 : size == len ? -1 : 1;
    }

   /**
     * Returns the internal byte array. This is unsafe unless you know what you're
     * doing. But it can improve performance for byte-array operations that
     * won't change the array.
     *
     * @return the internal byte array
     */
    public byte[] unsafeBytes() {
        return begin == 0 ? bytes : bytes();  
    }

    public byte[] bytes() {
        byte[] newBytes = new byte[realSize];
        System.arraycopy(bytes, begin, newBytes, 0, realSize);
        return newBytes;
    }

    public Object clone() {
        return dup();
    }
    
    public ByteList dup() {
        ByteList dup = new ByteList(false);
        dup.bytes = new byte[realSize];
        System.arraycopy(bytes, begin, dup.bytes, 0, realSize);
        dup.realSize = realSize;
        dup.begin = 0;
        return dup;        
    }
    
    public ByteList makeShared(int index, int len) {
        ByteList shared = new ByteList(false);        
        shared.bytes = bytes;
        shared.realSize = len;        
        shared.begin = begin + index;        
        return shared;
    }
    
    public void modify() {
        byte[] tmp = new byte[realSize];
        System.arraycopy(bytes, begin, tmp, 0, realSize);
        bytes = tmp;
        begin = 0;
    }
    
    public void view(int index, int len) {
        realSize = len;
        begin = begin + index;
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
    // leaving old version for now for comparisons, see 
    // TestByteList.java attached to JRUBY-xxx
    public int old_hashCode() {
        int key = 0;
        int g;
        for(int i = begin; i < begin + realSize; i++) {
            key = (key << 4) + (((int)bytes[i]) & 0xFF);
            if((g = key & 0xF0000000) != 0) {
                key ^= g >> 24;
            }
            key &= ~g;
        }
        return key;
    }

    public int hashCode() {
        int key = 0;
        int index = begin;
        final int end = begin + realSize; 
        while (index < end) {
            // equivalent of: key = key * 65599 + byte;
            key = ((key << 16) + (key << 6) - key) + (int)(bytes[index++]); // & 0xFF ? 
            }
        key = key + (key >> 5);
        return key;
    }
    
    public String toString() {
        return new String(plain(this.bytes, begin, realSize));
    }

    public static ByteList create(CharSequence s) {
        return new ByteList(plain(s),false);
    }

    public static byte[] plain(CharSequence s) {
        if(s instanceof String) {
            try {
                return ((String)s).getBytes("ISO8859-1");
            } catch(Exception e) {
                //FALLTHROUGH
            }
        }
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
    
    public static char[] plain(byte[] b, int start, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (b[start + i] & 0xFF);
        }
        return chars;
    }
    
    public static char[] plain(byte[] b) {
        char[] chars = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            chars[i] = (char) (b[i] & 0xFF);
        }
        return chars;
    }

    public char charAt(int ix) {
        return (char)(this.bytes[begin + ix] & 0xFF);
    }

    public CharSequence subSequence(int start, int end) {
        return new ByteList(this, start, end-start);
    }
}
