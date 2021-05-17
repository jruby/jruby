/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007-2010 JRuby Community
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jcodings.Encoding;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubyEncoding;
import org.jruby.runtime.Helpers;

/**
 * ByteList is simple a collection of bytes in the same way a Java String is a collection
 * of characters. However, its API resembles StringBuffer/StringBuilder more than String
 * because it is a mutable object.
 */
@SuppressWarnings("deprecation")
public class ByteList implements Comparable, CharSequence, Serializable {
    private static final long serialVersionUID = -1286166947275543731L;

    public static final byte[] NULL_ARRAY = new byte[0];
    public static final ByteList EMPTY_BYTELIST = new ByteList(NULL_ARRAY, false);

    private static final Charset ISO_LATIN_1 = StandardCharsets.ISO_8859_1;

    // NOTE: AR-JDBC (still) uses these fields directly in its ext .java parts  ,
    // until there's new releases we shall keep them public and maybe review other exts using BL's API
    @Deprecated
    public byte[] bytes;
    @Deprecated
    public int begin;
    @Deprecated
    public int realSize;

    private Encoding encoding = ASCIIEncoding.INSTANCE;

    transient int hash;

    transient String stringValue;

    private static final int DEFAULT_SIZE = 4;

    /**
     * Creates a new instance of ByteList
     */
    public ByteList() {
        this(DEFAULT_SIZE);
    }

    /**
     * Creates a new instance of Bytelist with a pre-allocated size.  If you know the size ahead
     * of time this saves additional array allocations to grow the bytelist to the proper size.
     *
     * @param size to preallocate the bytelist to
     */
    public ByteList(int size) {
        bytes = new byte[size];
        realSize = 0;
    }

    /**
     * Creates a new instance of Bytelist with a pre-allocated size and specified encoding.
     *
     * See {@link #ByteList(int)}
     *
     * @param size to preallocate the bytelist to
     * @param enc encoding to set
     */
    public ByteList(int size, Encoding enc) {
        bytes = new byte[size];
        realSize = 0;
        encoding = safeEncoding(enc);
    }

    /**
     * Create a new instance of ByteList with the bytes supplied using the specified encoding.
     *
     * Important: bytes is used as the initial backing store for the bytelist.  Over time as the
     * bytelist is mutated this backing store may be replaced with a new one to hold the additional
     * bytes.  If you pass in bytes and then modify the contents of the original bytes, then those
     * changes will get reflected.
     *
     * @param bytes to use
     * @param encoding
     */
    // TODO: Deprecate and replace with a static method which implies the caveats of this constructor.
    public ByteList(byte[] bytes, Encoding encoding) {
        this.bytes = bytes;
        this.realSize = bytes.length;
        this.encoding = safeEncoding(encoding);
    }

    /**
     * Create a new instance of ByteList with the contents of wrap.  This constructor will make
     * a copy of bytes passed
     *
     * @param wrap the initial bytes for this ByteList
     */
    public ByteList(byte[] wrap) {
        this(wrap, true);
    }

    /**
     * Create a new instance of ByteList with the contents of wrap.  If copy is true then it will
     * array copy the contents.  Otherwise it will use the byte array passed in as its initial
     * backing store.
     *
     * @param wrap the initial bytes for this ByteList
     * @param copy whether to arraycopy wrap for the backing store or not
     */
    public ByteList(byte[] wrap, boolean copy) {
        this(wrap, ASCIIEncoding.INSTANCE, copy);
    }

    /**
     * Create a new instance of ByteList with the contents of wrap.  If copy is true then it will
     * array copy the contents.  Otherwise it will use the byte array passed in as its initial
     * backing store.
     *
     * @param wrap the initial bytes for this ByteList
     * @param encoding the encoding for the bytes
     * @param copy whether to arraycopy wrap for the backing store or not
     */
    public ByteList(byte[] wrap, Encoding encoding, boolean copy) {
        assert wrap != null;
        if (copy) {
            this.bytes = wrap.clone();
        } else {
            this.bytes = wrap;
        }
        this.realSize = wrap.length;
        this.encoding = safeEncoding(encoding);
    }

    /**
     * Create a new instance of byte list with the same contents as the passed in ByteList wrap.
     * Note that this does array copy the data for the new objects initial backing store.
     *
     * @param wrap is contents for new ByteList
     */
    public ByteList(ByteList wrap) {
        this(wrap.bytes, wrap.begin, wrap.realSize, wrap.encoding, true);
    }

    /**
     * Create a new instance of ByteList using wrap as a backing store where index is the first
     * index in the byte array where the data starts and len indicates how long the data portion
     * of the bytelist is.  wrap will be array copied in this constructor.
     *
     * @param wrap the bytes to use
     * @param index where in the bytes the data starts
     * @param len how long the data is in the wrap array
     */
    public ByteList(byte[] wrap, int index, int len) {
        this(wrap, index, len, true);
    }

    /**
     * Create a new instance of ByteList using wrap as a backing store where index is the first
     * index in the byte array where the data starts and len indicates how long the data portion
     * of the bytelist is.  wrap will be array copied if copy is true OR if index != 0.
     *
     * @param wrap the bytes to use
     * @param index where in the bytes the data starts
     * @param len how long the data is in the wrap array
     * @param copy if true array copy wrap. otherwise use as backing store
     */
    public ByteList(byte[] wrap, int index, int len, boolean copy) {
        this(wrap, index, len, ASCIIEncoding.INSTANCE, copy);
    }

    /**
     * Create a new instance of ByteList using wrap as a backing store where index is the first
     * index in the byte array where the data starts and len indicates how long the data portion
     * of the bytelist is.  wrap will be array copied if copy is true OR if index != 0.
     *
     * @param wrap the bytes to use
     * @param index where in the bytes the data starts
     * @param len how long the data is in the wrap array
     * @param copy if true array copy wrap. otherwise use as backing store
     */
    public ByteList(byte[] wrap, int index, int len, Encoding encoding, boolean copy) {
        assert wrap != null : "'wrap' must not be null";
        assert index >= 0 && index <= wrap.length : "'index' is not without bounds of 'wrap' array";
        assert wrap.length >= index + len : "'index' + 'len' is longer than the 'wrap' array";

        if (copy) {
            bytes = new byte[len];
            System.arraycopy(wrap, index, bytes, 0, len);
        } else {
            begin = index;
            bytes = wrap;
        }
        realSize = len;
        this.encoding = safeEncoding(encoding);
    }

    /**
     * Create a new instance of ByteList using wrap as a backing store where index is the first
     * index in the byte array where the data starts and len indicates how long the data portion
     * of the bytelist is.  wrap's byte array will be array copied for initial backing store.
     *
     * @param wrap the bytes to use
     * @param index where in the bytes the data starts
     * @param len how long the data is in the wrap array
     */
    public ByteList(ByteList wrap, int index, int len) {
        this(wrap.bytes, wrap.begin + index, len);
        encoding = wrap.encoding;
    }

    /**
     * Delete len bytes from start index.  This does no bullet-proofing so it is your
     * responsibility to ensure you do not run off the backing store array.
     *
     * @param start index to delete from
     * @param len number of bytes to delete
     */
    public void delete(int start, int len) {
        assert start >= begin && start < realSize : "'start' is at invalid index";
        assert len >= 0 : "'len' must be positive";
        assert start + len <= begin + realSize : "too many bytes requested";

        realSize -= len;

        System.arraycopy(bytes, start + len, bytes, start, realSize);
        invalidate();
    }

    /**
     * Append the byte b up to len times onto the end of the current ByteList.
     *
     * @param b is byte to be appended
     * @param len is number of times to repeat the append
     */
    public void fill(final int b, final int len) {
        int i;
        switch (len) {
            case 0: return;
            case 1:
                grow(1);
                i = begin + realSize;
                bytes[i] = (byte) b;
                break;
            case 2:
                grow(2);
                i = begin + realSize;
                bytes[i] = (byte) b; bytes[i + 1] = (byte) b;
                break;
            case 3:
                grow(3);
                i = begin + realSize;
                bytes[i] = (byte) b; bytes[i + 1] = (byte) b; bytes[i + 2] = (byte) b;
                break;
            default:
                if (len < 0) return; // TODO: Sprintf assumes < 0 to work (likely should not)

                grow(len);
                i = begin + realSize;
                for (int s = len; --s >= 0; i++) bytes[i] = (byte) b;
        }
        realSize += len;
        invalidate();
    }

    /**
     * @see Object#clone()
     */
    @Override
    public Object clone() {
        return dup();
    }

    /**
     * creates a duplicate of this bytelist but only in the case of a stringValue and its resulting
     * hash value.  No other elements are duplicated.
     */
    public ByteList dup() {
        ByteList dup = dup(realSize);
        dup.hash = hash;
        dup.stringValue = stringValue;
        return dup;
    }

    /**
     * Create a new ByteList but do not array copy the byte backing store.
     *
     * @return a new ByteList with same backing store
     */
    public ByteList shallowDup() {
        ByteList dup = new  ByteList(bytes, false);
        dup.realSize = realSize;
        dup.begin = begin;
        dup.encoding = safeEncoding(encoding);
        dup.hash = hash;
        dup.stringValue = stringValue;
        return dup;
    }

    /**
     * @param length is the value of how big the buffer is going to be, not the actual length to copy
     *
     * It is used by RubyString.modify(int) to prevent COW pathological situations
     * (namely to COW with having <code>length - realSize</code> bytes ahead)
     */
    public ByteList dup(int length) {
        ByteList dup = new ByteList(length);

        dup.append(this.bytes, this.begin, this.realSize);
        dup.encoding = safeEncoding(encoding);

        return dup;
    }

    /**
     * Ensure that the bytelist is at least length bytes long.  Otherwise grow the backing store
     * so that it is at least length bytes long, but grow to 1.5 * length, if we're able
     * to, to avoid thrashing.
     *
     * @param length to use to make sure ByteList is long enough
     */
    public void ensure(int length) {
        if (begin + length > bytes.length) {
            byte[] tmp = new byte[Helpers.calculateBufferLength(length)];
            System.arraycopy(bytes, begin, tmp, 0, realSize);
            bytes = tmp;
            begin = 0;
            invalidate();
        }
    }

    /**
     * Make a shared copy of this ByteList.  This is used for COW'ing ByteLists, you typically
     * want a piece of the same backing store to be shared across ByteBuffers, while those
     * ByteLists will be pointing at different indexes and lengths of the same backing store.
     *
     * Note: that this does not update hash or stringValue.
     *
     * @param index new begin value for shared ByteBuffer
     * @param len new length/realSize for chared
     * @return
     */
    public ByteList makeShared(int index, int len) {
        ByteList shared = new ByteList(bytes, encoding);

        shared.realSize = len;
        shared.begin = begin + index;

        return shared;
    }

    /**
     * Change ByteBuffer to have a new begin that is +index positions past begin with a new length.
     *
     * @param index new value to add to begin
     * @param len the new realSize/length value
     */
    public void view(int index, int len) {
        realSize = len;
        begin = begin + index;
        invalidate();
    }

    /**
     * Array copy the byte backing store so that you can guarantee that no other objects are
     * referencing this objects backing store.
     */
    public void unshare() {
        unshare(realSize);
    }

    /**
     * Array copy the byte backing store so that you can guarantee that no other objects are
     * referencing this objects backing store.  This version on unshare allows a length to be
     * specified which will copy length bytes from the old backing store.
     *
     * @param length is the value of how big the buffer is going to be, not the actual length to copy
     *
     * It is used by RubyString.modify(int) to prevent COW pathological situations
     * (namely to COW with having <code>length - realSize</code> bytes ahead)
     */
    public void unshare(int length) {
        byte[] tmp = new byte[length];
        System.arraycopy(bytes, begin, tmp, 0, Math.min(realSize, length));
        bytes = tmp;
        begin = 0;
    }

    /**
     * Invalidate the hash and stringValue which may have been cached in this ByteList.
     */
    public void invalidate() {
        hash = 0;
        stringValue = null;
    }

    /**
     * Prepend a byte onto the front of this ByteList.
     *
     * @param b is the byte to be prepended
     */
    public void prepend(byte b) {
        grow(1);
        System.arraycopy(bytes, begin + 0, bytes, begin + 1, realSize);
        bytes[begin + 0] = b;
        realSize++;
        invalidate();
    }

    /**
     * Append a single byte to the ByteList
     *
     * @param b the byte to be added
     * @return this instance
     */
    public ByteList append(byte b) {
        grow(1);
        bytes[begin + realSize] = b;
        realSize++;
        invalidate();
        return this;
    }

    /**
     * Append a single byte to the ByteList by truncating the given int
     *
     * @param b the int to truncated and added
     * @return this instance
     */
    public ByteList append(int b) {
        append((byte)b);
        return this;
    }

    /**
     * Append up to length bytes from InputStream to the ByteList.   If no bytes are read from the
     * stream then throw an IOException.
     *
     * @param input the stream to read bytes from
     * @param len how many bytes to try reading
     * @return this instance
     * @throws IOException when no bytes are read
     */
    public ByteList append(InputStream input, int len) throws IOException {
        grow(len);
        int read = 0;
        int n;
        int start = begin + realSize;
        while (read < len) {
            n = input.read(bytes, start + read, len - read);
            if (n == -1) {
                if (read == 0) throw new java.io.EOFException();
                break;
            }
            read += n;
        }

        realSize += read;
        invalidate();
        return this;
    }

    /**
     * Append contents of the supplied nio ByteList up to len length onto the end of this
     * ByteBuffer.
     *
     * @param buffer to be appended
     * @param len is number of bytes you hoping to get from the ByteBuffer
     */
    public void append(ByteBuffer buffer, int len) {
        grow(len);
        buffer.get(bytes, begin + realSize, len);
        realSize += len;
        invalidate();
    }

    /**
     * Append moreBytes onto the end of the current ByteList.
     *
     * @param moreBytes to be added.
     */
    public void append(byte[] moreBytes) {
        assert moreBytes != null : "moreBytes is null";

        grow(moreBytes.length);
        System.arraycopy(moreBytes, 0, bytes, begin + realSize, moreBytes.length);
        realSize += moreBytes.length;
        invalidate();
    }

    /**
     * Append moreBytes onto the end of the current ByteList.
     *
     * @param moreBytes to be added.
     */
    public void append(ByteList moreBytes) {
        append(moreBytes.bytes, moreBytes.begin, moreBytes.realSize);
    }

    /**
     * Append moreBytes onto the end of the current ByteList with +index as the new begin for
     * len bytes from the moreBytes ByteList.
     *
     * @param moreBytes to be added.
     * @param index new index past current begin value
     * @param len is the number of bytes to append from source ByteList
     */
    public void append(ByteList moreBytes, int index, int len) {
        append(moreBytes.bytes, moreBytes.begin + index, len);
    }

    /**
     * Append moreBytes onto the end of the current ByteList with start as the new begin for
     * len bytes from the moreBytes byte array.
     *
     * @param moreBytes to be added.
     * @param start is the new begin value
     * @param len is the number of bytes to append from source byte array
     */
    public void append(byte[] moreBytes, int start, int len) {
        assert moreBytes != null : "moreBytes is null";
        assert start >= 0 && start <= moreBytes.length : "Invalid start: " + start;
        assert len >= 0 && moreBytes.length - start >= len : "Bad length: " + len;

        grow(len);
        System.arraycopy(moreBytes, start, bytes, begin + realSize, len);
        realSize += len;
        invalidate();
    }

    /**
     * Resize the ByteList's backing store to be length in size.  Note that this forces the backing
     * store to array copy regardless of ByteLists current size or contents.  It essentially will
     * end any COWing.
     *
     * @param length the new length for the backing store.
     */
    public void realloc(int length) {
        assert length >= 0 : "Invalid length";
        assert length >= realSize : "length is too small";

        byte tmp[] = new byte[length];
        System.arraycopy(bytes, 0, tmp, 0, realSize);
        bytes = tmp;
        invalidate();
    }

    /**
     * Return the current length of the ByteList.
     *
     * @return the number of bytes in this ByteList.
     */
    public int length() {
        return realSize;
    }

    /**
     * Return true if the ByteList has zero length (byte length).
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return realSize == 0;
    }

    /**
     * Set the byte container length.
     *
     * @param newLength
     */
    public void length(int newLength) {
        grow(newLength - realSize);
        realSize = newLength;
    }

    /**
     * Number of characters in this ByteList based on its current encoding.
     *
     * @return number of characters
     */
    public int lengthEnc() {
        return encoding.strLength(bytes, begin, begin + realSize);
    }

    /**
     * Get the byte at index from the ByteList.
     *
     * @param index to retreive byte from
     * @return the byte retreived
     */
    public int get(int index) {
        assert index >= 0 : "index must be positive";

        return bytes[begin + index] & 0xFF;
    }

    /**
     *  Get the index code point in this ByteList.
     *
     * @param index is the element you want
     * @return the element you requested
     */
    public int getEnc(int index) {
        return encoding.strCodeAt(bytes, begin, begin + realSize, index);
    }

    /**
     * Set the byte at index to be new value.
     *
     * @param index to set byte
     * @param b is the new value.
     */
    public void set(int index, int b) {
        assert index >= 0 : "index must be positive";
        assert begin + index < begin + realSize : "index is too large";

        bytes[begin + index] = (byte)b;
        invalidate();
    }

    /**
     * Unsafe version of replace(int,int,ByteList). The contract is that these
     * unsafe versions will not make sure thet beg and len indices are correct.
     */
    public void unsafeReplace(int beg, int len, ByteList nbytes) {
        unsafeReplace(beg, len, nbytes.bytes, nbytes.begin, nbytes.realSize);
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
        System.arraycopy(bytes, beg+len, bytes, beg+count, realSize - (len + beg));
        System.arraycopy(nbytes, index, bytes, beg, count);
        realSize = newSize;
        invalidate();
    }

    /**
     * Replace all bytes in this ByteList with bytes from the given array.
     *
     * @param source bytes to use for replacement
     */
    public void replace(byte[] source) {
        replace(0, realSize, source, 0, source.length);
    }

    /**
     * Replace a region of bytes in this ByteList with bytes from the given ByteList.
     *
     * @param targetOff offset of target region in this ByteList
     * @param targetLen length of target region in this ByteList
     * @param source ByteList to use for replacement
     */
    public void replace(int targetOff, int targetLen, ByteList source) {
        replace(targetOff, targetLen, source.bytes, source.begin, source.realSize);
    }

    /**
     * Replace a region of bytes in this ByteList with bytes from the given array.
     *
     * @param targetOff offset of target region in this ByteList
     * @param targetLen length of target region in this ByteList
     * @param source bytes to use for replacement
     */
    public void replace(int targetOff, int targetLen, byte[] source) {
        replace(targetOff, targetLen, source, 0, source.length);
    }

    /**
     * Replace a region of bytes in this ByteList with a region of bytes from the given array.
     *
     * @param targetOff offset of target region in this ByteList
     * @param targetLen length of target region in this ByteList
     * @param source bytes to use for replacement
     * @param sourceOff offset of source region in the replacement bytes
     * @param sourceLen length of source region in the replacement bytes
     */
    public void replace(int targetOff, int targetLen, byte[] source, int sourceOff, int sourceLen) {
        int newSize = realSize + (sourceLen - targetLen);
        grow(sourceLen - targetLen);
        int tailSize = realSize - (targetLen + targetOff);
        if (tailSize != 0) {
            System.arraycopy(bytes,begin+targetOff+targetLen,bytes,begin+targetOff+sourceLen,tailSize);
        }
        if (sourceLen != 0) {
            System.arraycopy(source,sourceOff,bytes,begin+targetOff,sourceLen);
        }
        realSize = newSize;
        invalidate();
    }

    public void insert(int index, int b) {
        grow(1);
        System.arraycopy(bytes, index, bytes, index + 1, realSize - index);
        bytes[index] = (byte)b;
        realSize++;
        invalidate();
    }

    /**
     * Get the index of first occurrence of c in ByteList from the beginning of the ByteList.
     *
     * @param c byte to be looking for
     * @return the index of the byte or -1 if not found
     */
    public int indexOf(int c) {
        return indexOf(c, 0);
    }

    /**
     * Get the index of first occurrence of c in ByteList from the pos offset of the ByteList.
     *
     * @param c byte to be looking for
     * @param pos off set from beginning of ByteList to look for byte
     * @return the index of the byte or -1 if not found
     */
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

    /**
     * Get the index of first occurrence of Bytelist find in this ByteList.
     *
     * @param find the ByteList to find
     * @return the index of the byte or -1 if not found
     */
    public int indexOf(ByteList find) {
        return indexOf(find, 0);
    }

    /**
     * Get the index of first occurrence of Bytelist find in this ByteList starting at index i.
     *
     * @param find the ByteList to find
     * @param i the index to start from
     * @return the index of the byte or -1 if not found
     */
    public int indexOf(ByteList find, int i) {
        return indexOf(bytes, begin, realSize, find.bytes, find.begin, find.realSize, i);
    }

    /**
     * Get the index of first occurrence of target in source using the offset and count parameters.
     * fromIndex can be used to start beyond zero on source.
     *
     * @return the index of the byte or -1 if not found
     */
    static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) return (targetCount == 0 ? sourceCount : -1);
        if (fromIndex < 0) fromIndex = 0;
        if (targetCount == 0) return fromIndex;

        byte first  = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            if (source[i] != first) while (++i <= max && source[i] != first);

            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);

                if (j == end) return i - sourceOffset;
            }
        }
        return -1;
    }

    /**
     * Get the index of last occurrence of c in ByteList from the end of the ByteList.
     *
     * @param c byte to be looking for
     * @return the index of the byte or -1 if not found
     */
    public int lastIndexOf(int c) {
        return lastIndexOf(c, realSize - 1);
    }

    /**
     * Get the index of last occurrence of c in ByteList from the pos offset of the ByteList.
     *
     * @param c byte to be looking for
     * @param pos off set from end of ByteList to look for byte
     * @return the index of the byte or -1 if not found
     */
    public int lastIndexOf(final int c, int pos) {
        // not sure if this is checked elsewhere,
        // didn't see it in RubyString. RubyString does
        // cast to char, so c will be >= 0.
        if (c > 255) return -1;

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

    /**
     * Get the index of last occurrence of find in ByteList from the end of the ByteList.
     *
     * @param find ByteList to be looking for
     * @return the index of the byte or -1 if not found
     */
    public int lastIndexOf(ByteList find) {
        return lastIndexOf(find, realSize);
    }

    /**
     * Get the index of last occurrence of find in ByteList from the end of the ByteList.
     *
     * @param find ByteList to be looking for
     * @param pos index from end of list to search from
     * @return the index of the byte or -1 if not found
     */
    public int lastIndexOf(ByteList find, int pos) {
        return lastIndexOf(bytes, begin, realSize, find.bytes, find.begin, find.realSize, pos);
    }

    /**
     * Get the index of last occurrence of target in source using the offset and count parameters.
     * fromIndex can be used to start beyond zero on source.
     *
     * @return the index of the byte or -1 if not found
     */
    static int lastIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        int rightIndex = sourceCount - targetCount;
        if (fromIndex < 0) return -1;
        if (fromIndex > rightIndex) fromIndex = rightIndex;
        if (targetCount == 0) return fromIndex;

        int strLastIndex = targetOffset + targetCount - 1;
        byte strLastChar = target[strLastIndex];
        int min = sourceOffset + targetCount - 1;
        int i = min + fromIndex;

        startSearchForLastChar:
        while (true) {
            while (i >= min && source[i] != strLastChar) i--;
            if (i < min) return -1;
            int j = i - 1;
            int start = j - (targetCount - 1);
            int k = strLastIndex - 1;

            while (j > start) {
                if (source[j--] != target[k--]) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start - sourceOffset + 1;
        }
    }

    public boolean startsWith(ByteList other, int toffset) {
        if (realSize == 0 || this.realSize < other.realSize + toffset) return false;

        byte[]ta = bytes;
        int to = begin + toffset;
        byte[]pa = other.bytes;
        int po = other.begin;
        int pc = other.realSize;

        while (--pc >= 0) if (ta[to++] != pa[po++]) return false;
        return true;
    }

    /**
     * Does this ByteList start with the supplied ByteList?
     *
     * @param other is the bytelist to compare with
     * @return true is this ByteList starts with other
     */
    public boolean startsWith(ByteList other) {
        return startsWith(other, 0);
    }

    /**
     * Does this ByteList end with the supplied ByteList?
     *
     * @param other is the bytelist to compare with
     * @return true is this ByteList starts with other
     */
    public boolean endsWith(ByteList other) {
        return startsWith(other, realSize - other.realSize);
    }

    /**
     * Does this ByteList equal the other ByteList?
     *
     * @param other is the bytelist to compare with
     * @return true is this ByteList is the same
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ByteList) return equal((ByteList)other);
        return false;
    }

    /**
     * Does this ByteList equal the other ByteList?
     *
     * @param other is the bytelist to compare with
     * @return true is this ByteList is the same
     */
    public boolean equal(ByteList other) {
        if (other == this) return true;
        if (hash != 0 && other.hash != 0 && hash != other.hash) return false;

        int first, last;
        if ((last = realSize) == other.realSize) {
            byte buf[] = bytes;
            byte otherBuf[] = other.bytes;
            // scanning from front and back simultaneously, meeting in
            // the middle. the object is to get a mismatch as quickly as
            // possible. alternatives might be: scan from the middle outward
            // (not great because it won't pick up common variations at the
            // ends until late) or sample odd bytes forward and even bytes
            // backward (I like this one, but it's more expensive for
            // strings that are equal; see sample_equals below).

            for (first = -1;
                 --last > first && buf[begin + last] == otherBuf[other.begin + last] &&
                         ++first < last && buf[begin + first] == otherBuf[other.begin + first] ; ) ;
            return first >= last;
        }
        return false;
    }

    /**
     * an alternative to the new version of equals, should
     * detect inequality faster (in many cases), but is slow
     * in the case of equal values (all bytes visited), due to
     * using n+=2, n-=2 vs. ++n, --n while iterating over the array.
     */
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

    /**
     * This comparison matches MRI comparison of Strings (rb_str_cmp).
     */
    public int cmp(final ByteList other) {
        if (other == this) return 0;
        final int size = realSize;
        final int len =  Math.min(size, other.realSize);
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
     * Do a case insensitive comparison with other ByteList with return types similiar to compareTo.
     *
     * @param other the ByteList to compare
     * @return -1, 0, or 1
     */
    public int caseInsensitiveCmp(final ByteList other) {
        if (other == this) return 0;

        final int size = realSize;
        final int len =  Math.min(size, other.realSize);
        final int other_begin = other.begin;
        final byte[] other_bytes = other.bytes;

        for (int offset = -1; ++offset < len;) {
            int myCharIgnoreCase = AsciiTables.ToLowerCaseTable[bytes[begin + offset] & 0xff] & 0xff;
            int otherCharIgnoreCase = AsciiTables.ToLowerCaseTable[other_bytes[other_begin + offset] & 0xff] & 0xff;
            if (myCharIgnoreCase < otherCharIgnoreCase) {
                return -1;
            } else if (myCharIgnoreCase > otherCharIgnoreCase) {
                return 1;
            }
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
        return bytes;
    }

    /**
     * Get a copy of the bytes referenced by this ByteList.  It will make an optimal copy and not
     * carry along unused bytes from COW sharing.
     *
     * @return a copy of the bytes.
     */
    public byte[] bytes() {
        byte[] newBytes = new byte[realSize];
        System.arraycopy(bytes, begin, newBytes, 0, realSize);
        return newBytes;
    }

    /**
     * First index of the backing array that contains data for the ByteList.  Note that we have
     * copy-on-write (COW) semantics which means sharing the same backing store will yield different
     * begin and size values while using the same byte[].
     *
     * @return the index
     */
    public int begin() {
        return begin;
    }

    /**
     * Grow the ByteList by increaseRequested bytes.
     *
     * @param increaseRequested number of bytes to grow
     */
    private void grow(int increaseRequested) {
        // new available size
        try {
            int newSize = Math.addExact(realSize, increaseRequested); // increase <= 0 -> no-op
            // only recopy if bytes does not have enough room *after* the begin index
            if (newSize > bytes.length - begin) {
                byte[] newBytes = new byte[Helpers.calculateBufferLength(newSize)];
                if (bytes.length != 0) System.arraycopy(bytes, begin, newBytes, 0, realSize);
                bytes = newBytes;
                begin = 0;
            }
        } catch (ArithmeticException ae) {
            throw new OutOfMemoryError("Requested array size exceeds VM limit");
        }
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = this.hash;
        if (hash != 0) return hash;

        hash = 0;
        int begin = this.begin, realSize = this.realSize;
        byte[] bytes = this.bytes;

        int index = begin;
        final int end = begin + realSize;
        while (index < end) {
            // equivalent of: hash = hash * 65599 + byte;
            hash = ((hash << 16) + (hash << 6) - hash) + (int)(bytes[index++]); // & 0xFF ?
        }
        hash = hash + (hash >> 5);
        return this.hash = hash;
    }

    /**
     * Remembers toString value, which is expensive for StringBuffer.
     *
     * @return an ISO-8859-1 representation of the byte list
     */
    @Override
    public String toString() {
        String decoded = this.stringValue;
        if (decoded == null) {
            this.stringValue = decoded = decode(bytes, begin, realSize, ISO_LATIN_1);
        }
        return decoded;
    }

    /**
     * Produce a String using the raw (ISO-8859-1) bytes of this ByteList.
     *
     * @return a String based on the raw bytes
     */
    public String toByteString() {
        return new String(bytes, begin, realSize, ISO_LATIN_1);
    }

    /**
     * Create a bytelist with ISO_8859_1 encoding from the provided CharSequence.
     *
     * @param s the source for new ByteList
     * @return the new ByteList
     */
    public static ByteList create(CharSequence s) {
        return new ByteList(plain(s),false);
    }

    /**
     * Create a byte[] from a CharSequence assuming a raw/ISO-8859-1 encoding
     *
     * @param s the CharSequence to convert
     * @return a byte[]
     */
    public static byte[] plain(CharSequence s) {
        return RubyEncoding.encodeISO(s);
    }

    /**
     * Create a byte[] from a char[] assuming a raw/ISO-8859-1 encoding
     *
     * @param s the CharSequence to convert
     * @return a byte[]
     */
    public static byte[] plain(char[] s) {
        byte[] bytes = new byte[s.length];
        for (int i = 0; i < s.length; i++) {
            bytes[i] = (byte) s[i];
        }
        return bytes;
    }

    /**
     * Create a char[] from a byte[] assuming a raw/ISO-8859-1 encoding
     *
     * @param b the source byte[]
     * @param start index to start converting to char's
     * @param length how many bytes to convert to char's
     * @return a byte[]
     */
    public static char[] plain(byte[] b, int start, int length) {
        assert b != null : "byte array cannot be null";
        assert start >= 0 && start + length <= b.length : "Invalid start or start+length too long";

        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (b[start + i] & 0xFF);
        }
        return chars;
    }

    /**
     * Create a char[] from a byte[] assuming a raw/ISO-8859-1 encoding
     *
     * @param b the source byte[]
     * @return a byte[]
     */
    public static char[] plain(byte[] b) {
        assert b != null : "byte array cannot be null";

        char[] chars = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            chars[i] = (char) (b[i] & 0xFF);
        }
        return chars;
    }

    // Work around bad charset handling in JDK. See
    // http://halfbottle.blogspot.com/2009/07/charset-continued-i-wrote-about.html
    private static final ConcurrentMap<String,Charset> charsetsByAlias = new ConcurrentHashMap<>();

    /**
     * Decode byte data into a String with the supplied charsetName.
     *
     * @param data to be decoded
     * @param offset where to start decoding from in data
     * @param length how many bytes to decode from data
     * @param charsetName used to make the resulting String
     * @return the new String
     */
    public static String decode(byte[] data, int offset, int length, String charsetName) {
        return decode(data, offset, length, lookup(charsetName));
    }

    private static String decode(byte[] data, int offset, int length, Charset charset) {
        return charset.decode(ByteBuffer.wrap(data, offset, length)).toString();
    }

    /**
     * Decode byte data into a String with the supplied charsetName.
     *
     * @param data to be decoded
     * @param charsetName used to make the resulting String
     * @return the new String
     */
    public static String decode(byte[] data, String charsetName) {
        return lookup(charsetName).decode(ByteBuffer.wrap(data)).toString();
    }

    /**
     * Encode CharSequence into a set of bytes based on the charsetName.
     *
     * @param data to be encoded
     * @param charsetName used to extract the resulting bytes
     * @return the new byte[]
     */
    public static byte[] encode(CharSequence data, String charsetName) {
        return lookup(charsetName).encode(CharBuffer.wrap(data)).array();
    }

    private static Charset lookup(String alias) {
        Charset cs = charsetsByAlias.get(alias);
        if (cs == null) {
            cs = Charset.forName(alias);
            charsetsByAlias.putIfAbsent(alias, cs);
        }
        return cs;
    }

    /**
     * Pretend byte array is raw and each byte is also the character value
     *
     * @param ix is the index you want
     * @return
     */
    public char charAt(int ix) {
        return (char)(this.bytes[begin + ix] & 0xFF);
    }

    /**
     * Create subSequence of this array between start and end offsets
     *
     * @param start index for beginning of subsequence
     * @param end index for end of subsequence
     * @return a new ByteList/CharSequence
     */
    public CharSequence subSequence(int start, int end) {
        return new ByteList(this, start, end - start);
    }

    /**
     * Are these two byte arrays similiar (semantics similiar to compareTo).  This is slightly
     * special in that it will only compare the same number of bytes based on the lesser of the two
     * lengths.
     *
     * @return -1, 0, 1
     */
    public static int memcmp(final byte[] first, final int firstStart, final int firstLen, final byte[] second, final int secondStart, final int secondLen) {
        if (first == second) return 0;
        final int len =  Math.min(firstLen,secondLen);
        int offset = -1;
        for (  ; ++offset < len && first[firstStart + offset] == second[secondStart + offset]; ) ;
        if (offset < len) {
            return (first[firstStart + offset]&0xFF) > (second[secondStart + offset]&0xFF) ? 1 : -1;
        }
        return firstLen == secondLen ? 0 : firstLen == len ? -1 : 1;

    }

    /**
     * Are these two byte arrays similiar (semantics similiar to compareTo).
     *
     * @return -1, 0, 1
     */
    public static int memcmp(final byte[] first, final int firstStart, final byte[] second, final int secondStart, int len) {
        int a = firstStart;
        int b = secondStart;
        int tmp;

        for (; len != 0; --len) {
            if ((tmp = first[a++] - second[b++]) != 0) {
                return tmp;
            }
        }
        return 0;
    }


    /**
     * @return the bytes
     */
    public byte[] getUnsafeBytes() {
        return bytes;
    }

    /**
     * @param bytes the bytes to set
     */
    public void setUnsafeBytes(byte[] bytes) {
        assert bytes != null;
        this.bytes = bytes;
        invalidate();
    }

    /**
     * @return the begin
     */
    public int getBegin() {
        return begin;
    }

    /**
     * @param begin the begin to set
     */
    public void setBegin(int begin) {
        assert begin >= 0;
        this.begin = begin;
        invalidate();
    }

    /**
     * @return the realSize
     */
    public int getRealSize() {
        return realSize();
    }

    /**
     * @param realSize the realSize to set
     */
    public void setRealSize(int realSize) {
        realSize(realSize);
    }

    /**
     * @return the realSize
     */
    public int realSize() { return realSize; }

    /**
     * @param realSize the realSize to set
     */
    public void realSize(int realSize) {
        assert realSize >= 0;
        this.realSize = realSize;
        invalidate();
    }

    /**
     * @return the encoding
     */
    public Encoding getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(Encoding encoding) {
        assert encoding != null;
        this.encoding = safeEncoding(encoding);
        invalidate();
    }

    /**
     * Ensure the encoding is always non-null.
     */
    public static Encoding safeEncoding(Encoding incoming) {
        if (incoming == null) return ASCIIEncoding.INSTANCE;
        return incoming;
    }
}
