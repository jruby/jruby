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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * A WriterOutputStream converts java.io.Writer to java.io.OutputStream.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class WriterOutputStream extends OutputStream {

    private final Writer writer;
    private boolean isOpen = true;
    private CharsetDecoder decoder;

    /**
     * Creates WriterOutputStream from given java.io.Writer object with a default encoding.
     *
     * @param writer java.io.Writer object to be converted to.
     */
    public WriterOutputStream(Writer writer) {
        this(writer, null);
    }

    /**
     * Creates WriterOutputStream from given java.io.Writer object with a specified encoding.
     *
     * @param writer java.io.Writer object to be converted to.
     */
    public WriterOutputStream(Writer writer, String encoding) {
        this.writer = writer;       
        if (encoding == null && writer instanceof OutputStreamWriter) {
            // this encoding might be null when writer has been closed
            encoding = ((OutputStreamWriter) writer).getEncoding();
        }
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        } else if (!Charset.isSupported(encoding)) {
            throw new IllegalArgumentException(encoding + " is not supported");
        }
        decoder = Charset.forName(encoding).newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The general contract of <code>close</code>
     * is that it closes the output stream. A closed stream cannot perform
     * output operations and cannot be reopened.
     * <p>
     * 
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        synchronized (writer) {
            if (!isOpen) {
                throw new IOException("This stream has been already closed.");
            }
            isOpen = false;
            decoder = null;
            writer.close();
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out. The general contract of <code>flush</code> is
     * that calling it is an indication that, if any bytes previously
     * written have been buffered by the implementation of the output
     * stream, such bytes should immediately be written to their
     * intended destination.
     * <p>
     * If the intended destination of this stream is an abstraction provided by
     * the underlying operating system, for example a file, then flushing the
     * stream guarantees only that bytes previously written to the stream are
     * passed to the operating system for writing; it does not guarantee that
     * they are actually written to a physical device such as a disk drive.
     * <p>
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        synchronized (writer) {
            if (!isOpen) {
                return;
            }
            writer.flush();
        }
    }

    /**
     * Writes the specified byte to this output stream. The general
     * contract for <code>write</code> is that one byte is written
     * to the output stream. The byte to be written is the eight
     * low-order bits of the argument <code>b</code>. The 24
     * high-order bits of <code>b</code> are ignored.
     *
     * @param      b   the <code>byte</code>.
     * @exception  IOException  if an I/O error occurs. In particular,
     *             an <code>IOException</code> may be thrown if the
     *             output stream has been closed.
     */
    @Override
    public void write(int b) throws IOException {
        byte[] bb = new byte[]{(byte) b};
        write(bb, 0, 1);
    }

    /**
     * Writes <code>b.length</code> bytes from the specified byte array
     * to this output stream. The general contract for <code>write(b)</code>
     * is that it should have exactly the same effect as the call
     * <code>write(b, 0, b.length)</code>.
     *
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     * The general contract for <code>write(b, off, len)</code> is that
     * some of the bytes in the array <code>b</code> are written to the
     * output stream in order; element <code>b[off]</code> is the first
     * byte written and <code>b[off+len-1]</code> is the last byte written
     * by this operation.
     * <p>
     * If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs. In particular,
     *             an <code>IOException</code> is thrown if the output
     *             stream is closed.
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (writer) {
            if (!isOpen) {
                return;
            }
            if (off < 0 || len <= 0 || (off + len) > b.length) {
                throw new IndexOutOfBoundsException();
            }
            ByteBuffer bytes = ByteBuffer.wrap(b, off, len);
            CharBuffer chars = CharBuffer.allocate(len);
            byte2char(bytes, chars);
            char[] cbuf = new char[chars.length()];
            chars.get(cbuf, 0, chars.length());
            writer.write(cbuf);
            writer.flush();
        }
    }

    private void byte2char(ByteBuffer bytes, CharBuffer chars) throws IOException {
        decoder.reset();
        chars.clear();
        CoderResult result = decoder.decode(bytes, chars, true);
        if (result.isError() || result.isOverflow()) {
            throw new IOException(result.toString());
        } else if (result.isUnderflow()) {
            chars.flip();
        }
    }
}
