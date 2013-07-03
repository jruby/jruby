/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009-2010 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;

/**
 * A ReaderInputStream converts java.io.Reader to java.io.InputStream. The
 * ReaderInputStream reads data in a given Reader object into its internal buffer
 * so that users of this class can access the file that Reader read by using methods
 * defined in java.io.InputStream.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class ReaderInputStream extends InputStream {

    private static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    private final Reader reader;
    private byte[] bytes = null;
    private int totalBytes = 0;
    private int position = 0;
    private int markedIndex = -1;
    private int readlimit = 0;
    private boolean isOpen = true;
    private CharsetEncoder encoder;
    private final Object lock = new Object();

    /**
     * Creates ReaderInputStream from a given Reader type object with a default encoding.
     *
     * @param reader java.io.Reader object to be read data from.
     */
    public ReaderInputStream(Reader reader) {
        this(reader, null);
    }

    /**
     * Creates ReaderInputStream from a given Reader type object with a specifed encoding.
     *
     * @param reader java.io.Reader object to be read data from.
     * @param encoding an encoding of the created stream.
     */
    public ReaderInputStream(Reader reader, String encoding) {
        this.reader = reader;
        if (encoding == null) {
            if (reader instanceof InputStreamReader) {
                encoding = ((InputStreamReader) reader).getEncoding();
            } else {
                encoding = Charset.defaultCharset().name();
            }
        } else if (!Charset.isSupported(encoding)) {
            throw new IllegalArgumentException(encoding + " is not supported");
        }
        encoder = Charset.forName(encoding).newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPLACE);
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            fillByteBuffer(reader);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void fillByteBuffer(Reader reader) throws IOException {
        CharBuffer cbuf = CharBuffer.allocate(DEFAULT_CHAR_BUFFER_SIZE);
        ByteBuffer bbuf = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
        List<byte[]> list = new ArrayList<byte[]>();
        while (true) {
            cbuf.clear();
            int size = reader.read(cbuf);
            if (size <= 0) {
                break;
            }
            cbuf.limit(cbuf.position());
            cbuf.rewind();
            boolean eof = false;
            while (!eof) {
                CoderResult cr = encoder.encode(cbuf, bbuf, eof);
                if (cr.isError()) {
                    cr.throwException();
                } else if (cr.isUnderflow()) {
                    appendBytes(list, bbuf);
                    eof = true;
                } else if (cr.isOverflow()) {
                    appendBytes(list, bbuf);
                    bbuf.clear();
                }
            }
        }
        getByteArray(list);
    }

    private void appendBytes(List<byte[]> list, ByteBuffer bb) {
        bb.flip();
        int length = bb.limit();
        totalBytes += length;
        byte[] dst = new byte[length];
        System.arraycopy(bb.array(), bb.position(), dst, 0, length);
        list.add(dst);
    }

    private void getByteArray(List<byte[]> list) {
        bytes = new byte[totalBytes];
        int index = 0;
        for (byte[] bb : list) {
            for (int i=0; i<bb.length; i++) {
                bytes[index++] = bb[i];
            }
        }
    }

    private void confirmOpen() throws IOException {
        if (!isOpen) {
            throw new IOException("This stream has been closed.");
        }
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation
     * might be the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     *
     * <p> Note that while some implementations of {@code InputStream} will return
     * the total number of bytes in the stream, many will not.  It is
     * never correct to use the return value of this method to allocate
     * a buffer intended to hold all data in this stream.
     *
     * <p> A subclass' implementation of this method may choose to throw an
     * {@link IOException} if this input stream has been closed by
     * invoking the {@link #close()} method.
     *
     * <p> The {@code available} method for class {@code InputStream} always
     * returns {@code 0}.
     *
     * @return     an estimate of the number of bytes that can be read (or skipped
     *             over) from this input stream without blocking or {@code 0} when
     *             it reaches the end of the input stream.
     * @exception  IOException if an I/O error occurs.
     */
    @Override
    public int available() throws IOException {
        synchronized (lock) {
            confirmOpen();
            if (bytes == null) {
                throw new IOException("This stream is not available.");
            }
            return totalBytes - position;
        }
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            confirmOpen();
            isOpen = false;
            encoder = null;
            bytes = null;
            //reader.close();
        }
    }

    /**
     * Marks the current position in this input stream. A subsequent call to
     * the <code>reset</code> method repositions this stream at the last marked
     * position so that subsequent reads re-read the same bytes.
     *
     * <p> The <code>readlimit</code> arguments tells this input stream to
     * allow that many bytes to be read before the mark position gets
     * invalidated.
     *
     * <p> The general contract of <code>mark</code> is that, if the method
     * <code>markSupported</code> returns <code>true</code>, the stream somehow
     * remembers all the bytes read after the call to <code>mark</code> and
     * stands ready to supply those same bytes again if and whenever the method
     * <code>reset</code> is called.  However, the stream is not required to
     * remember any data at all if more than <code>readlimit</code> bytes are
     * read from the stream before <code>reset</code> is called.
     *
     * <p> Marking a closed stream should not have any effect on the stream.
     *
     * <p> The <code>mark</code> method of <code>InputStream</code> does
     * nothing.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     * @see     java.io.InputStream#reset()
     */
    @Override
    public synchronized void mark(int readlimit) {
        if (readlimit < 0) {
            throw new IllegalArgumentException("Read limit < 0");
        }
        synchronized (lock) {
            if (isOpen) {
                this.readlimit = readlimit;
                markedIndex = position;
            }
        }
    }

    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods. Whether or not <code>mark</code> and
     * <code>reset</code> are supported is an invariant property of a
     * particular input stream instance.
     *
     * @return  <code>true</code> if this stream instance supports the mark
     *          and reset methods; <code>false</code> otherwise.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * <p> A subclass must provide an implementation of this method.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        synchronized (lock) {
            confirmOpen();
            if (position >= totalBytes) {
                return -1;
            } else {
                return bytes[position++];
            }
        }
    }

    /**
     * Reads some number of bytes from the input stream and stores them into
     * the buffer array <code>b</code>. The number of bytes actually read is
     * returned as an integer.  This method blocks until input data is
     * available, end of file is detected, or an exception is thrown.
     *
     * <p> If the length of <code>b</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at the
     * end of the file, the value <code>-1</code> is returned; otherwise, at
     * least one byte is read and stored into <code>b</code>.
     *
     * <p> The first byte read is stored into element <code>b[0]</code>, the
     * next one into <code>b[1]</code>, and so on. The number of bytes read is,
     * at most, equal to the length of <code>b</code>. Let <i>k</i> be the
     * number of bytes actually read; these bytes will be stored in elements
     * <code>b[0]</code> through <code>b[</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[</code><i>k</i><code>]</code> through
     * <code>b[b.length-1]</code> unaffected.
     *
     * <p> The <code>read(b)</code> method for class <code>InputStream</code>
     * has the same effect as: <pre><code> read(b, 0, b.length) </code></pre>
     *
     * @param      b   the buffer into which the data is read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> is there is no more data because the end of
     *             the stream has been reached.
     * @exception  IOException  If the first byte cannot be read for any reason
     * other than the end of the file, if the input stream has been closed, or
     * if some other I/O error occurs.
     * @exception  NullPointerException  if <code>b</code> is <code>null</code>.
     * @see        java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     *
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     *
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     *
     * <p> The <code>read(b,</code> <code>off,</code> <code>len)</code> method
     * for class <code>InputStream</code> simply calls the method
     * <code>read()</code> repeatedly. If the first such call results in an
     * <code>IOException</code>, that exception is returned from the call to
     * the <code>read(b,</code> <code>off,</code> <code>len)</code> method.  If
     * any subsequent call to <code>read()</code> results in a
     * <code>IOException</code>, the exception is caught and treated as if it
     * were end of file; the bytes read up to that point are stored into
     * <code>b</code> and the number of bytes read before the exception
     * occurred is returned. The default implementation of this method blocks
     * until the requested amount of input data <code>len</code> has been read,
     * end of file is detected, or an exception is thrown. Subclasses are encouraged
     * to provide a more efficient implementation of this method.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     * @exception  IOException If the first byte cannot be read for any reason
     * other than end of file, or if the input stream has been closed, or if
     * some other I/O error occurs.
     * @exception  NullPointerException If <code>b</code> is <code>null</code>.
     * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
     * <code>len</code> is negative, or <code>len</code> is greater than
     * <code>b.length - off</code>
     * @see        java.io.InputStream#read()
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            confirmOpen();
            if (len == 0) {
                return 0;
            }
            if (position >= totalBytes) {
                return -1;
            }
            if (off < 0 || off > totalBytes || len < 0) {
                throw new IllegalArgumentException("Either one of, or both of off and len are invalid.");
            }
            int start = position + off;
            start = start < totalBytes ? start : totalBytes - 1;
            int end = start + len;
            end = end < totalBytes ? end : totalBytes - 1;
            int actuallyRead = Math.min((end - start) + 1, len);
            System.arraycopy(bytes, start, b, 0, actuallyRead);
            position += actuallyRead;
            return actuallyRead;

        }
    }

    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     *
     * <p> The general contract of <code>reset</code> is:
     *
     * <p><ul>
     *
     * <li> If the method <code>markSupported</code> returns
     * <code>true</code>, then:
     *
     *     <ul><li> If the method <code>mark</code> has not been called since
     *     the stream was created, or the number of bytes read from the stream
     *     since <code>mark</code> was last called is larger than the argument
     *     to <code>mark</code> at that last call, then an
     *     <code>IOException</code> might be thrown.
     *
     *     <li> If such an <code>IOException</code> is not thrown, then the
     *     stream is reset to a state such that all the bytes read since the
     *     most recent call to <code>mark</code> (or since the start of the
     *     file, if <code>mark</code> has not been called) will be resupplied
     *     to subsequent callers of the <code>read</code> method, followed by
     *     any bytes that otherwise would have been the next input data as of
     *     the time of the call to <code>reset</code>. </ul>
     *
     * <li> If the method <code>markSupported</code> returns
     * <code>false</code>, then:
     *
     *     <ul><li> The call to <code>reset</code> may throw an
     *     <code>IOException</code>.
     *
     *     <li> If an <code>IOException</code> is not thrown, then the stream
     *     is reset to a fixed state that depends on the particular type of the
     *     input stream and how it was created. The bytes that will be supplied
     *     to subsequent callers of the <code>read</code> method depend on the
     *     particular type of the input stream. </ul></ul>
     *
     * @exception  IOException  if this stream has not been marked or if the
     *               mark has been invalidated.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.IOException
     */
    @Override
    public synchronized void reset() throws IOException {
        synchronized (lock) {
            if (!isOpen) {
                throw new IOException("This stream has been closed.");
            }
            if (markedIndex < 0) {
                throw new IOException("This stream is not marked.");
            }
            if ((position - markedIndex) > readlimit) {
                throw new IOException("Mark is invalidated.");
            }
            position = markedIndex;
        }
    }

    /**
     * Skips over and discards <code>n</code> bytes of data from this input
     * stream. The <code>skip</code> method may, for a variety of reasons, end
     * up skipping over some smaller number of bytes, possibly <code>0</code>.
     * This may result from any of a number of conditions; reaching end of file
     * before <code>n</code> bytes have been skipped is only one possibility.
     * The actual number of bytes skipped is returned.  If <code>n</code> is
     * negative, no bytes are skipped.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     * @exception  IOException  if the stream does not support seek,
     *                          or if some other I/O error occurs.
     */
    @Override
    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("Negarive skip");
        }
        synchronized (lock) {
            if (!isOpen) {
                throw new IOException("This stream has been closed.");
            }
            long skipped;
            if ((totalBytes - position) < n) {
                skipped = totalBytes - position;
                position = totalBytes;
            } else {
                skipped = n;
                position += n;
            }
            return skipped;
        }
    }
}
