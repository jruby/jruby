/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Damian Steer <pldms@mac.com>
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
package org.jruby.util.io;


import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;

import org.jruby.Finalizable;
import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyFile;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.nio.channels.spi.SelectorProvider;

/**
 * This file implements a seekable IO file.
 */
public class ChannelStream implements Stream, Finalizable {

    private static final Logger LOG = LoggerFactory.getLogger("ChannelStream");

    private final static boolean DEBUG = false;

    /**
     * The size of the read/write buffer allocated for this stream.
     *
     * This size has been scaled back from its original 16k because although
     * the larger buffer size results in raw File.open times being rather slow
     * (due to the cost of instantiating a relatively large buffer). We should
     * try to find a happy medium, or potentially pool buffers, or perhaps even
     * choose a value based on platform(??), but for now I am reducing it along
     * with changes for the "large read" patch from JRUBY-2657.
     */
    public final static int BUFSIZE = 4 * 1024;

    /**
     * The size at which a single read should turn into a chunkier bulk read.
     * Currently, this size is about 4x a normal buffer size.
     *
     * This size was not really arrived at experimentally, and could potentially
     * be increased. However, it seems like a "good size" and we should
     * probably only adjust it if it turns out we would perform better with a
     * larger buffer for large bulk reads.
     */
    private final static int BULK_READ_SIZE = 16 * 1024;
    private final static ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private volatile Ruby runtime;
    protected ModeFlags modes;
    protected boolean sync = false;

    protected volatile ByteBuffer buffer; // r/w buffer
    protected boolean reading; // are we reading or writing?
    private ChannelDescriptor descriptor;
    private boolean blocking = true;
    protected int ungotc = -1;
    private volatile boolean closedExplicitly = false;

    private volatile boolean eof = false;
    private volatile boolean autoclose = true;

    private ChannelStream(Ruby runtime, ChannelDescriptor descriptor, boolean autoclose) {
        this.runtime = runtime;
        this.descriptor = descriptor;
        this.modes = descriptor.getOriginalModes();
        buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.flip();
        this.reading = true;
        this.autoclose = autoclose;
        runtime.addInternalFinalizer(this);
    }

    private ChannelStream(Ruby runtime, ChannelDescriptor descriptor, ModeFlags modes, boolean autoclose) {
        this(runtime, descriptor, autoclose);
        this.modes = modes;
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public void checkReadable() throws IOException {
        if (!modes.isReadable()) throw new IOException("not opened for reading");
    }

    public void checkWritable() throws IOException {
        if (!modes.isWritable()) throw new IOException("not opened for writing");
    }

    public void checkPermissionsSubsetOf(ModeFlags subsetModes) {
        subsetModes.isSubsetOf(modes);
    }

    public ModeFlags getModes() {
    	return modes;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public void setBinmode() {
        // No-op here, no binmode handling needed.
    }
    
    public boolean isBinmode() {
        return false;
    }

    public boolean isAutoclose() {
        return autoclose;
    }

    public void setAutoclose(boolean autoclose) {
        this.autoclose = autoclose;
    }

    /**
     * Implement IO#wait as per io/wait in MRI.
     * waits until input available or timed out and returns self, or nil when EOF reached.
     *
     * The default implementation loops while ready returns 0.
     */
    public void waitUntilReady() throws IOException, InterruptedException {
        while (ready() == 0) {
            Thread.sleep(10);
        }
    }

    public boolean readDataBuffered() {
        return reading && (ungotc != -1 || buffer.hasRemaining());
    }

    public boolean writeDataBuffered() {
        return !reading && buffer.position() > 0;
    }

    private final int refillBuffer() throws IOException {
        buffer.clear();
        int n = ((ReadableByteChannel) descriptor.getChannel()).read(buffer);
        buffer.flip();
        return n;
    }
    public synchronized ByteList fgets(ByteList separatorString) throws IOException, BadDescriptorException {
        checkReadable();
        ensureRead();

        if (separatorString == null) {
            return readall();
        }

        final ByteList separator = (separatorString == PARAGRAPH_DELIMETER) ?
            PARAGRAPH_SEPARATOR : separatorString;

        descriptor.checkOpen();

        if (feof()) {
            return null;
        }

        int c = read();

        if (c == -1) {
            return null;
        }

        // unread back
        buffer.position(buffer.position() - 1);

        ByteList buf = new ByteList(40);

        byte first = separator.getUnsafeBytes()[separator.getBegin()];

        LineLoop : while (true) {
            ReadLoop: while (true) {
                byte[] bytes = buffer.array();
                int offset = buffer.position();
                int max = buffer.limit();

                // iterate over remainder of buffer until we find a match
                for (int i = offset; i < max; i++) {
                    c = bytes[i];
                    if (c == first) {
                        // terminate and advance buffer when we find our char
                        buf.append(bytes, offset, i - offset);
                        if (i >= max) {
                            buffer.clear();
                        } else {
                            buffer.position(i + 1);
                        }
                        break ReadLoop;
                    }
                }

                // no match, append remainder of buffer and continue with next block
                buf.append(bytes, offset, buffer.remaining());
                int read = refillBuffer();
                if (read == -1) break LineLoop;
            }

            // found a match above, check if remaining separator characters match, appending as we go
            for (int i = 0; i < separator.getRealSize(); i++) {
                if (c == -1) {
                    break LineLoop;
                } else if (c != separator.getUnsafeBytes()[separator.getBegin() + i]) {
                    buf.append(c);
                    continue LineLoop;
                }
                buf.append(c);
                if (i < separator.getRealSize() - 1) {
                    c = read();
                }
            }
            break;
        }

        if (separatorString == PARAGRAPH_DELIMETER) {
            while (c == separator.getUnsafeBytes()[separator.getBegin()]) {
                c = read();
            }
            ungetc(c);
        }

        return buf;
    }

    public synchronized int getline(ByteList dst, byte terminator) throws IOException, BadDescriptorException {
        checkReadable();
        ensureRead();
        descriptor.checkOpen();

        int totalRead = 0;
        boolean found = false;
        if (ungotc != -1) {
            dst.append((byte) ungotc);
            found = ungotc == terminator;
            ungotc = -1;
            ++totalRead;
        }
        while (!found) {
            final byte[] bytes = buffer.array();
            final int begin = buffer.arrayOffset() + buffer.position();
            final int end = begin + buffer.remaining();
            int len = 0;
            for (int i = begin; i < end && !found; ++i) {
                found = bytes[i] == terminator;
                ++len;
            }
            if (len > 0) {
                dst.append(buffer, len);
                totalRead += len;
            }
            if (!found) {
                int n = refillBuffer();
                if (n <= 0) {
                    if (n < 0 && totalRead < 1) {
                        return -1;
                    }
                    break;
                }
            }
        }
        return totalRead;
    }

    public synchronized int getline(ByteList dst, byte terminator, long limit) throws IOException, BadDescriptorException {
        checkReadable();
        ensureRead();
        descriptor.checkOpen();

        int totalRead = 0;
        boolean found = false;
        if (ungotc != -1) {
            dst.append((byte) ungotc);
            found = ungotc == terminator;
            ungotc = -1;
            limit--;
            ++totalRead;
        }
        while (!found) {
            final byte[] bytes = buffer.array();
            final int begin = buffer.arrayOffset() + buffer.position();
            final int end = begin + buffer.remaining();
            int len = 0;
            for (int i = begin; i < end && limit-- > 0 && !found; ++i) {
                found = bytes[i] == terminator;
                ++len;
            }
            if (limit < 1) found = true;

            if (len > 0) {
                dst.append(buffer, len);
                totalRead += len;
            }
            if (!found) {
                int n = refillBuffer();
                if (n <= 0) {
                    if (n < 0 && totalRead < 1) {
                        return -1;
                    }
                    break;
                }
            }
        }
        return totalRead;
    }

    /**
     * @deprecated readall do busy loop for the IO which has NONBLOCK bit. You
     *             should implement the logic by yourself with fread().
     */
    @Deprecated
    public synchronized ByteList readall() throws IOException, BadDescriptorException {
        final long fileSize = descriptor.isSeekable() && descriptor.getChannel() instanceof FileChannel
                ? ((FileChannel) descriptor.getChannel()).size() : 0;
        //
        // Check file size - special files in /proc have zero size and need to be
        // handled by the generic read path.
        //
        if (fileSize > 0) {
            ensureRead();

            FileChannel channel = (FileChannel)descriptor.getChannel();
            final long left = fileSize - channel.position() + bufferedInputBytesRemaining();
            if (left <= 0) {
                eof = true;
                return null;
            }

            if (left > Integer.MAX_VALUE) {
                if (getRuntime() != null) {
                    throw getRuntime().newIOError("File too large");
                } else {
                    throw new IOException("File too large");
                }
            }

            ByteList result = new ByteList((int) left);
            ByteBuffer buf = ByteBuffer.wrap(result.getUnsafeBytes(),
                    result.begin(), (int) left);

            //
            // Copy any buffered data (including ungetc byte)
            //
            copyBufferedBytes(buf);

            //
            // Now read unbuffered directly from the file
            //
            while (buf.hasRemaining()) {
                final int MAX_READ_CHUNK = 1 * 1024 * 1024;
                //
                // When reading into a heap buffer, the jvm allocates a temporary
                // direct ByteBuffer of the requested size.  To avoid allocating
                // a huge direct buffer when doing ludicrous reads (e.g. 1G or more)
                // we split the read up into chunks of no more than 1M
                //
                ByteBuffer tmp = buf.duplicate();
                if (tmp.remaining() > MAX_READ_CHUNK) {
                    tmp.limit(tmp.position() + MAX_READ_CHUNK);
                }
                int n = channel.read(tmp);
                if (n <= 0) {
                    break;
                }
                buf.position(tmp.position());
            }
            eof = true;
            result.length(buf.position());
            return result;
        } else if (descriptor.isNull()) {
            return new ByteList(0);
        } else {
            checkReadable();

            ByteList byteList = new ByteList();
            ByteList read = fread(BUFSIZE);

            if (read == null) {
                eof = true;
                return byteList;
            }

            while (read != null) {
                byteList.append(read);
                read = fread(BUFSIZE);
            }

            return byteList;
        }
    }

    /**
     * Copies bytes from the channel buffer into a destination <tt>ByteBuffer</tt>
     *
     * @param dst A <tt>ByteBuffer</tt> to place the data in.
     * @return The number of bytes copied.
     */
    private final int copyBufferedBytes(ByteBuffer dst) {
        final int bytesToCopy = dst.remaining();

        if (ungotc != -1 && dst.hasRemaining()) {
            dst.put((byte) ungotc);
            ungotc = -1;
        }

        if (buffer.hasRemaining() && dst.hasRemaining()) {

            if (dst.remaining() >= buffer.remaining()) {
                //
                // Copy out any buffered bytes
                //
                dst.put(buffer);

            } else {
                //
                // Need to clamp source (buffer) size to avoid overrun
                //
                ByteBuffer tmp = buffer.duplicate();
                tmp.limit(tmp.position() + dst.remaining());
                dst.put(tmp);
                buffer.position(tmp.position());
            }
        }

        return bytesToCopy - dst.remaining();
    }

    /**
     * Copies bytes from the channel buffer into a destination <tt>ByteBuffer</tt>
     *
     * @param dst A <tt>ByteBuffer</tt> to place the data in.
     * @return The number of bytes copied.
     */
    private final int copyBufferedBytes(byte[] dst, int off, int len) {
        int bytesCopied = 0;

        if (ungotc != -1 && len > 0) {
            dst[off++] = (byte) ungotc;
            ungotc = -1;
            ++bytesCopied;
        }

        final int n = Math.min(len - bytesCopied, buffer.remaining());
        buffer.get(dst, off, n);
        bytesCopied += n;

        return bytesCopied;
    }

    /**
     * Copies bytes from the channel buffer into a destination <tt>ByteBuffer</tt>
     *
     * @param dst A <tt>ByteList</tt> to place the data in.
     * @param len The maximum number of bytes to copy.
     * @return The number of bytes copied.
     */
    private final int copyBufferedBytes(ByteList dst, int len) {
        int bytesCopied = 0;

        dst.ensure(Math.min(len, bufferedInputBytesRemaining()));

        if (bytesCopied < len && ungotc != -1) {
            ++bytesCopied;
            dst.append((byte) ungotc);
            ungotc = -1;
        }

        //
        // Copy out any buffered bytes
        //
        if (bytesCopied < len && buffer.hasRemaining()) {
            int n = Math.min(buffer.remaining(), len - bytesCopied);
            dst.append(buffer, n);
            bytesCopied += n;
        }

        return bytesCopied;
    }

    /**
     * Returns a count of how many bytes are available in the read buffer
     *
     * @return The number of bytes that can be read without reading the underlying stream.
     */
    private final int bufferedInputBytesRemaining() {
        return reading ? (buffer.remaining() + (ungotc != -1 ? 1 : 0)) : 0;
    }

    /**
     * Tests if there are bytes remaining in the read buffer.
     *
     * @return <tt>true</tt> if there are bytes available in the read buffer.
     */
    private final boolean hasBufferedInputBytes() {
        return reading && (buffer.hasRemaining() || ungotc != -1);
    }

    /**
     * Returns a count of how many bytes of space is available in the write buffer.
     *
     * @return The number of bytes that can be written to the buffer without flushing
     * to the underlying stream.
     */
    private final int bufferedOutputSpaceRemaining() {
        return !reading ? buffer.remaining() : 0;
    }

    /**
     * Tests if there is space available in the write buffer.
     *
     * @return <tt>true</tt> if there are bytes available in the write buffer.
     */
    private final boolean hasBufferedOutputSpace() {
        return !reading && buffer.hasRemaining();
    }

    /**
     * Closes IO handler resources.
     *
     * @throws IOException
     * @throws BadDescriptorException
     */
    public void fclose() throws IOException, BadDescriptorException {
        try {
            synchronized (this) {
                closedExplicitly = true;
                close(); // not closing from finalize
            }
        } finally {
            Ruby localRuntime = getRuntime();

            // Make sure we remove finalizers while not holding self lock,
            // otherwise there is a possibility for a deadlock!
            if (localRuntime != null) localRuntime.removeInternalFinalizer(this);

            // clear runtime so it doesn't get stuck in memory (JRUBY-2933)
            runtime = null;
        }
    }

    /**
     * Internal close.
     *
     * @throws IOException
     * @throws BadDescriptorException
     */
    private void close() throws IOException, BadDescriptorException {
        // finish and close ourselves
        finish(true);
    }

    private void finish(boolean close) throws BadDescriptorException, IOException {
        try {
            flushWrite();

            if (DEBUG) LOG.info("Descriptor for fileno {} closed by stream", descriptor.getFileno());
        } finally {
            buffer = EMPTY_BUFFER;

            // clear runtime so it doesn't get stuck in memory (JRUBY-2933)
            runtime = null;

            // finish descriptor
            descriptor.finish(close);
        }
    }

    /**
     * @throws IOException
     * @throws BadDescriptorException
     */
    public synchronized int fflush() throws IOException, BadDescriptorException {
        checkWritable();
        try {
            flushWrite();
        } catch (EOFException eofe) {
            return -1;
        }
        return 0;
    }

    /**
     * Flush the write buffer to the channel (if needed)
     * @throws IOException
     */
    private void flushWrite() throws IOException, BadDescriptorException {
        if (reading || !modes.isWritable() || buffer.position() == 0) return; // Don't bother

        int len = buffer.position();
        buffer.flip();
        int n = descriptor.write(buffer);

        if(n != len) {
            // TODO: check the return value here
        }
        buffer.clear();
    }

    /**
     * Flush the write buffer to the channel (if needed)
     * @throws IOException
     */
    private boolean flushWrite(final boolean block) throws IOException, BadDescriptorException {
        if (reading || !modes.isWritable() || buffer.position() == 0) return false; // Don't bother
        int len = buffer.position();
        int nWritten = 0;
        buffer.flip();

        // For Sockets, only write as much as will fit.
        if (descriptor.getChannel() instanceof SelectableChannel) {
            SelectableChannel selectableChannel = (SelectableChannel)descriptor.getChannel();
            synchronized (selectableChannel.blockingLock()) {
                boolean oldBlocking = selectableChannel.isBlocking();
                try {
                    if (oldBlocking != block) {
                        selectableChannel.configureBlocking(block);
                    }
                    nWritten = descriptor.write(buffer);
                } finally {
                    if (oldBlocking != block) {
                        selectableChannel.configureBlocking(oldBlocking);
                    }
                }
            }
        } else {
            nWritten = descriptor.write(buffer);
        }
        if (nWritten != len) {
            buffer.compact();
            return false;
        }
        buffer.clear();
        return true;
    }

    public InputStream newInputStream() {
        InputStream in = descriptor.getBaseInputStream();
        return in == null ? new InputStreamAdapter(this) : in;
    }

    public OutputStream newOutputStream() {
        return new OutputStreamAdapter(this);
    }

    public void clearerr() {
        eof = false;
    }

    /**
     * @throws IOException
     * @throws BadDescriptorException
     */
    public boolean feof() throws IOException, BadDescriptorException {
        checkReadable();

        if (eof) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @throws IOException
     */
    public synchronized long fgetpos() throws IOException, PipeException, InvalidValueException, BadDescriptorException {
        // Correct position for read / write buffering (we could invalidate, but expensive)
        if (descriptor.isSeekable()) {
            FileChannel fileChannel = (FileChannel)descriptor.getChannel();
            long pos = fileChannel.position();
            // Adjust for buffered data
            if (reading) {
                pos -= buffer.remaining();
                return pos - (pos > 0 && ungotc != -1 ? 1 : 0);
            } else {
                return pos + buffer.position();
            }
        } else if (descriptor.isNull()) {
            return 0;
        } else {
            throw new PipeException();
        }
    }

    /**
     * Implementation of libc "lseek", which seeks on seekable streams, raises
     * EPIPE if the fd is assocated with a pipe, socket, or FIFO, and doesn't
     * do anything for other cases (like stdio).
     *
     * @throws IOException
     * @throws InvalidValueException
     */
    public synchronized void lseek(long offset, int type) throws IOException, InvalidValueException, PipeException, BadDescriptorException {
        if (descriptor.isSeekable()) {
            FileChannel fileChannel = (FileChannel)descriptor.getChannel();
            ungotc = -1;
            int adj = 0;
            if (reading) {
                // for SEEK_CUR, need to adjust for buffered data
                adj = buffer.remaining();
                buffer.clear();
                buffer.flip();
            } else {
                flushWrite();
            }
            try {
                switch (type) {
                case SEEK_SET:
                    fileChannel.position(offset);
                    break;
                case SEEK_CUR:
                    fileChannel.position(fileChannel.position() - adj + offset);
                    break;
                case SEEK_END:
                    fileChannel.position(fileChannel.size() + offset);
                    break;
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException();
            } catch (IOException ioe) {
                throw ioe;
            }
        } else if (descriptor.getChannel() instanceof SelectableChannel) {
            // TODO: It's perhaps just a coincidence that all the channels for
            // which we should raise are instanceof SelectableChannel, since
            // stdio is not...so this bothers me slightly. -CON
            throw new PipeException();
        } else {
        }
    }

    public synchronized void sync() throws IOException, BadDescriptorException {
        flushWrite();
    }

    /**
     * Ensure buffer is ready for reading, flushing remaining writes if required
     * @throws IOException
     */
    private void ensureRead() throws IOException, BadDescriptorException {
        if (reading) return;
        flushWrite();
        buffer.clear();
        buffer.flip();
        reading = true;
    }

    /**
     * Ensure buffer is ready for reading, flushing remaining writes if required
     * @throws IOException
     */
    private void ensureReadNonBuffered() throws IOException, BadDescriptorException {
        if (reading) {
            if (buffer.hasRemaining()) {
                Ruby localRuntime = getRuntime();
                if (localRuntime != null) {
                    throw localRuntime.newIOError("sysread for buffered IO");
                } else {
                    throw new IOException("sysread for buffered IO");
                }
            }
        } else {
            // libc flushes writes on any read from the actual file, so we flush here
            flushWrite();
            buffer.clear();
            buffer.flip();
            reading = true;
        }
    }

    private void resetForWrite() throws IOException {
        if (descriptor.isSeekable()) {
            FileChannel fileChannel = (FileChannel)descriptor.getChannel();
            if (buffer.hasRemaining()) { // we have read ahead, and need to back up
                fileChannel.position(fileChannel.position() - buffer.remaining());
            }
        }
        // FIXME: Clearing read buffer here...is this appropriate?
        buffer.clear();
        reading = false;
    }

    /**
     * Ensure buffer is ready for writing.
     * @throws IOException
     */
    private void ensureWrite() throws IOException {
        if (!reading) return;
        resetForWrite();
    }

    public synchronized ByteList read(int number) throws IOException, BadDescriptorException {
        checkReadable();
        ensureReadNonBuffered();

        ByteList byteList = new ByteList(number);

        // TODO this should entry into error handling somewhere
        int bytesRead = descriptor.read(number, byteList);

        if (bytesRead == -1) {
            eof = true;
        }

        return byteList;
    }

    private ByteList bufferedRead(int number) throws IOException, BadDescriptorException {
        checkReadable();
        ensureRead();

        int resultSize = 0;

        // 128K seems to be the minimum at which the stat+seek is faster than reallocation
        final int BULK_THRESHOLD = 128 * 1024;
        if (number >= BULK_THRESHOLD && descriptor.isSeekable() && descriptor.getChannel() instanceof FileChannel) {
            //
            // If it is a file channel, then we can pre-allocate the output buffer
            // to the total size of buffered + remaining bytes in file
            //
            FileChannel fileChannel = (FileChannel) descriptor.getChannel();
            resultSize = (int) Math.min(fileChannel.size() - fileChannel.position() + bufferedInputBytesRemaining(), number);
        } else {
            //
            // Cannot discern the total read length - allocate at least enough for the buffered data
            //
            resultSize = Math.min(bufferedInputBytesRemaining(), number);
        }

        ByteList result = new ByteList(resultSize);
        bufferedRead(result, number);
        return result;
    }

    private int bufferedRead(ByteList dst, int number) throws IOException, BadDescriptorException {

        int bytesRead = 0;

        //
        // Copy what is in the buffer, if there is some buffered data
        //
        bytesRead += copyBufferedBytes(dst, number);

        boolean done = false;
        //
        // Avoid double-copying for reads that are larger than the buffer size
        //
        while ((number - bytesRead) >= BUFSIZE) {
            //
            // limit each iteration to a max of BULK_READ_SIZE to avoid over-size allocations
            //
            final int bytesToRead = Math.min(BULK_READ_SIZE, number - bytesRead);
            final int n = descriptor.read(bytesToRead, dst);
            if (n == -1) {
                eof = true;
                done = true;
                break;
            } else if (n == 0) {
                done = true;
                break;
            }
            bytesRead += n;
        }

        //
        // Complete the request by filling the read buffer first
        //
        while (!done && bytesRead < number) {
            int read = refillBuffer();

            if (read == -1) {
                eof = true;
                break;
            } else if (read == 0) {
                break;
            }

            // append what we read into our buffer and allow the loop to continue
            final int len = Math.min(buffer.remaining(), number - bytesRead);
            dst.append(buffer, len);
            bytesRead += len;
        }

        if (bytesRead == 0 && number != 0) {
            if (eof) {
                throw new EOFException();
            }
        }

        return bytesRead;
    }

    private int bufferedRead(ByteBuffer dst, boolean partial) throws IOException, BadDescriptorException {
        checkReadable();
        ensureRead();

        boolean done = false;
        int bytesRead = 0;

        //
        // Copy what is in the buffer, if there is some buffered data
        //
        bytesRead += copyBufferedBytes(dst);

        //
        // Avoid double-copying for reads that are larger than the buffer size, or
        // the destination is a direct buffer.
        //
        while ((bytesRead < 1 || !partial) && (dst.remaining() >= BUFSIZE || dst.isDirect())) {
            ByteBuffer tmpDst = dst;
            if (!dst.isDirect()) {
                //
                // We limit reads to BULK_READ_SIZED chunks to avoid NIO allocating
                // a huge temporary native buffer, when doing reads into a heap buffer
                // If the dst buffer is direct, then no need to limit.
                //
                int bytesToRead = Math.min(BULK_READ_SIZE, dst.remaining());
                if (bytesToRead < dst.remaining()) {
                    tmpDst = dst.duplicate();
                    tmpDst.limit(tmpDst.position() + bytesToRead);
                }
            }
            int n = descriptor.read(tmpDst);
            if (n == -1) {
                eof = true;
                done = true;
                break;
            } else if (n == 0) {
                done = true;
                break;
            } else {
                bytesRead += n;
            }
        }

        //
        // Complete the request by filling the read buffer first
        //
        while (!done && dst.hasRemaining() && (bytesRead < 1 || !partial)) {
            int read = refillBuffer();

            if (read == -1) {
                eof = true;
                done = true;
                break;
            } else if (read == 0) {
                done = true;
                break;
            } else {
                // append what we read into our buffer and allow the loop to continue
                bytesRead += copyBufferedBytes(dst);
            }
        }

        if (eof && bytesRead == 0 && dst.remaining() != 0) {
            throw new EOFException();
        }

        return bytesRead;
    }

    private int bufferedRead() throws IOException, BadDescriptorException {
        ensureRead();

        if (!buffer.hasRemaining()) {
            int len = refillBuffer();
            if (len == -1) {
                eof = true;
                return -1;
            } else if (len == 0) {
                return -1;
            }
        }
        return buffer.get() & 0xFF;
    }

    /**
     * @throws IOException
     * @throws BadDescriptorException
     */
    private int bufferedWrite(ByteList buf) throws IOException, BadDescriptorException {
        checkWritable();
        ensureWrite();

        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) return 0;

        if (buf.length() > buffer.capacity()) { // Doesn't fit in buffer. Write immediately.
            flushWrite(); // ensure nothing left to write


            int n = descriptor.write(ByteBuffer.wrap(buf.getUnsafeBytes(), buf.begin(), buf.length()));
            if(n != buf.length()) {
                // TODO: check the return value here
            }
        } else {
            if (buf.length() > buffer.remaining()) flushWrite();

            buffer.put(buf.getUnsafeBytes(), buf.begin(), buf.length());
        }

        if (isSync()) flushWrite();

        return buf.getRealSize();
    }

    /**
     * @throws IOException
     * @throws BadDescriptorException
     */
    private int bufferedWrite(ByteBuffer buf) throws IOException, BadDescriptorException {
        checkWritable();
        ensureWrite();

        // Ruby ignores empty syswrites
        if (buf == null || !buf.hasRemaining()) return 0;

        final int nbytes = buf.remaining();
        if (nbytes >= buffer.capacity()) { // Doesn't fit in buffer. Write immediately.
            flushWrite(); // ensure nothing left to write

            descriptor.write(buf);
            // TODO: check the return value here
        } else {
            if (nbytes > buffer.remaining()) flushWrite();

            buffer.put(buf);
        }

        if (isSync()) flushWrite();

        return nbytes - buf.remaining();
    }

    /**
     * @throws IOException
     * @throws BadDescriptorException
     */
    private int bufferedWrite(int c) throws IOException, BadDescriptorException {
        checkWritable();
        ensureWrite();

        if (!buffer.hasRemaining()) flushWrite();

        buffer.put((byte) c);

        if (isSync()) flushWrite();

        return 1;
    }

    public synchronized void ftruncate(long newLength) throws IOException,
            BadDescriptorException, InvalidValueException {
        Channel ch = descriptor.getChannel();
        if (!(ch instanceof FileChannel)) {
            throw new InvalidValueException();
        }
        invalidateBuffer();
        FileChannel fileChannel = (FileChannel)ch;
        if (newLength > fileChannel.size()) {
            // truncate can't lengthen files, so we save position, seek/write, and go back
            long position = fileChannel.position();
            int difference = (int)(newLength - fileChannel.size());

            fileChannel.position(fileChannel.size());
            // FIXME: This worries me a bit, since it could allocate a lot with a large newLength
            fileChannel.write(ByteBuffer.allocate(difference));
            fileChannel.position(position);
        } else {
            fileChannel.truncate(newLength);
        }
    }

    /**
     * Invalidate buffer before a position change has occurred (e.g. seek),
     * flushing writes if required, and correcting file position if reading
     * @throws IOException
     */
    private void invalidateBuffer() throws IOException, BadDescriptorException {
        if (!reading) flushWrite();
        int posOverrun = buffer.remaining(); // how far ahead we are when reading
        buffer.clear();
        if (reading) {
            buffer.flip();
            // if the read buffer is ahead, back up
            FileChannel fileChannel = (FileChannel)descriptor.getChannel();
            if (posOverrun != 0) fileChannel.position(fileChannel.position() - posOverrun);
        }
    }

    /**
     * Ensure close (especially flush) when we're finished with.
     */
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        
        if (closedExplicitly) return;

        if (DEBUG) {
            LOG.info("finalize() for not explicitly closed stream");
        }

        // FIXME: I got a bunch of NPEs when I didn't check for nulls here...HOW?!
        if (descriptor != null && descriptor.isOpen()) {
            // tidy up
            finish(autoclose);
        }
    }

    public int ready() throws IOException {
        if (descriptor.getChannel() instanceof SelectableChannel) {
            int ready_stat = 0;
            java.nio.channels.Selector sel = SelectorFactory.openWithRetryFrom(null, ((SelectableChannel) descriptor.getChannel()).provider());
            SelectableChannel selchan = (SelectableChannel)descriptor.getChannel();
            synchronized (selchan.blockingLock()) {
                boolean is_block = selchan.isBlocking();
                try {
                    selchan.configureBlocking(false);
                    selchan.register(sel, java.nio.channels.SelectionKey.OP_READ);
                    ready_stat = sel.selectNow();
                    sel.close();
                } catch (Throwable ex) {
                } finally {
                    if (sel != null) {
                        try {
                            sel.close();
                        } catch (Exception e) {
                        }
                    }
                    selchan.configureBlocking(is_block);
                }
            }
            return ready_stat;
        } else {
            return newInputStream().available();
        }
    }

    public synchronized void fputc(int c) throws IOException, BadDescriptorException {
        bufferedWrite(c);
    }

    public int ungetc(int c) {
        if (c == -1) {
            return -1;
        }

        // putting a bit back, so we're not at EOF anymore
        eof = false;

        // save the ungot
        ungotc = c;

        return c;
    }

    public synchronized int fgetc() throws IOException, BadDescriptorException {
        if (eof) {
            return -1;
        }

        checkReadable();

        int c = read();

        if (c == -1) {
            eof = true;
            return c;
        }

        return c & 0xff;
    }

    public synchronized int fwrite(ByteList string) throws IOException, BadDescriptorException {
        return bufferedWrite(string);
    }

    public synchronized int write(ByteBuffer buf) throws IOException, BadDescriptorException {
        return bufferedWrite(buf);
    }

    public synchronized int writenonblock(ByteList buf) throws IOException, BadDescriptorException {
        checkWritable();
        ensureWrite();

        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) return 0;

        if (buffer.position() != 0 && !flushWrite(false)) return 0;

        if (descriptor.getChannel() instanceof SelectableChannel) {
            SelectableChannel selectableChannel = (SelectableChannel)descriptor.getChannel();
            synchronized (selectableChannel.blockingLock()) {
                boolean oldBlocking = selectableChannel.isBlocking();
                try {
                    if (oldBlocking) {
                        selectableChannel.configureBlocking(false);
                    }
                    return descriptor.write(ByteBuffer.wrap(buf.getUnsafeBytes(), buf.begin(), buf.length()));
                } finally {
                    if (oldBlocking) {
                        selectableChannel.configureBlocking(oldBlocking);
                    }
                }
            }
        } else {
            // can't set nonblocking, so go ahead with it...not much else we can do
            return descriptor.write(ByteBuffer.wrap(buf.getUnsafeBytes(), buf.begin(), buf.length()));
        }
    }

    public synchronized ByteList fread(int number) throws IOException, BadDescriptorException {
        try {
            if (number == 0) {
                if (eof) {
                    return null;
                } else {
                    return new ByteList(0);
                }
            }

            return bufferedRead(number);
        } catch (EOFException e) {
            eof = true;
            return null;
        }
    }

    public synchronized ByteList readnonblock(int number) throws IOException, BadDescriptorException, EOFException {
        assert number >= 0;

        if (number == 0) {
            return null;
        }

        if (descriptor.getChannel() instanceof SelectableChannel) {
            SelectableChannel selectableChannel = (SelectableChannel)descriptor.getChannel();
            synchronized (selectableChannel.blockingLock()) {
                boolean oldBlocking = selectableChannel.isBlocking();
                try {
                    selectableChannel.configureBlocking(false);
                    return readpartial(number);
                } finally {
                    selectableChannel.configureBlocking(oldBlocking);
                }
            }
        } else if (descriptor.getChannel() instanceof FileChannel) {
            return fread(number);
        } else {
            return null;
        }
    }

    public synchronized ByteList readpartial(int number) throws IOException, BadDescriptorException, EOFException {
        assert number >= 0;

        if (number == 0) {
            return null;
        }
        if (descriptor.getChannel() instanceof FileChannel) {
            return fread(number);
        }

        if (hasBufferedInputBytes()) {
            // already have some bytes buffered, just return those
            return bufferedRead(Math.min(bufferedInputBytesRemaining(), number));
        } else {
            // otherwise, we try an unbuffered read to get whatever's available
            return read(number);
        }
    }

    public synchronized int read(ByteBuffer dst) throws IOException, BadDescriptorException, EOFException {
        return read(dst, !(descriptor.getChannel() instanceof FileChannel));
    }

    public synchronized int read(ByteBuffer dst, boolean partial) throws IOException, BadDescriptorException, EOFException {
        assert dst.hasRemaining();

        return bufferedRead(dst, partial);
    }

    public synchronized int read() throws IOException, BadDescriptorException {
        try {
            descriptor.checkOpen();

            if (ungotc >= 0) {
                int c = ungotc;
                ungotc = -1;
                return c;
            }

            return bufferedRead();
        } catch (EOFException e) {
            eof = true;
            return -1;
        }
    }

    public ChannelDescriptor getDescriptor() {
        return descriptor;
    }

    public void setBlocking(boolean block) throws IOException {
        if (!(descriptor.getChannel() instanceof SelectableChannel)) {
            return;
        }
        synchronized (((SelectableChannel) descriptor.getChannel()).blockingLock()) {
            blocking = block;
            try {
                ((SelectableChannel) descriptor.getChannel()).configureBlocking(block);
            } catch (IllegalBlockingModeException e) {
                // ignore this; select() will set the correct mode when it is finished
            }
        }
    }

    public boolean isBlocking() {
        return blocking;
    }

    public synchronized void freopen(Ruby runtime, String path, ModeFlags modes) throws DirectoryAsFileException, IOException, InvalidValueException, PipeException, BadDescriptorException {
        // flush first
        flushWrite();

        // reset buffer
        buffer.clear();
        if (reading) {
            buffer.flip();
        }

        this.modes = modes;

        if (descriptor.isOpen()) {
            descriptor.close();
        }

        if (path.equals("/dev/null") || path.equalsIgnoreCase("nul:") || path.equalsIgnoreCase("nul")) {
            descriptor = descriptor.reopen(new NullChannel(), modes);
        } else {
            String cwd = runtime.getCurrentDirectory();
            JRubyFile theFile = JRubyFile.create(cwd,path);

            if (theFile.isDirectory() && modes.isWritable()) throw new DirectoryAsFileException();

            if (modes.isCreate()) {
                if (theFile.exists() && modes.isExclusive()) {
                    throw runtime.newErrnoEEXISTError("File exists - " + path);
                }
                theFile.createNewFile();
            } else {
                if (!theFile.exists()) {
                    throw runtime.newErrnoENOENTError("file not found - " + path);
                }
            }

            // We always open this rw since we can only open it r or rw.
            RandomAccessFile file = new RandomAccessFile(theFile, modes.toJavaModeString());

            if (modes.isTruncate()) file.setLength(0L);

            descriptor = descriptor.reopen(file, modes);

            try {
                if (modes.isAppendable()) lseek(0, SEEK_END);
            } catch (PipeException pe) {
                // ignore, it's a pipe or fifo
            }
        }
    }

    public static Stream open(Ruby runtime, ChannelDescriptor descriptor) {
        return maybeWrapWithLineEndingWrapper(new ChannelStream(runtime, descriptor, true), descriptor.getOriginalModes());
    }

    public static Stream fdopen(Ruby runtime, ChannelDescriptor descriptor, ModeFlags modes) throws InvalidValueException {
        // check these modes before constructing, so we don't finalize the partially-initialized stream
        descriptor.checkNewModes(modes);
        return maybeWrapWithLineEndingWrapper(new ChannelStream(runtime, descriptor, modes, true), modes);
    }

    public static Stream open(Ruby runtime, ChannelDescriptor descriptor, boolean autoclose) {
        return maybeWrapWithLineEndingWrapper(new ChannelStream(runtime, descriptor, autoclose), descriptor.getOriginalModes());
    }

    public static Stream fdopen(Ruby runtime, ChannelDescriptor descriptor, ModeFlags modes, boolean autoclose) throws InvalidValueException {
        // check these modes before constructing, so we don't finalize the partially-initialized stream
        descriptor.checkNewModes(modes);
        return maybeWrapWithLineEndingWrapper(new ChannelStream(runtime, descriptor, modes, autoclose), modes);
    }

    private static Stream maybeWrapWithLineEndingWrapper(Stream stream, ModeFlags modes) {
        if (Platform.IS_WINDOWS && stream.getDescriptor().getChannel() instanceof FileChannel && !modes.isBinary()) {
            return new CRLFStreamWrapper(stream);
        }
        return stream;
    }

    public static Stream fopen(Ruby runtime, String path, ModeFlags modes) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException, InvalidValueException, PipeException, BadDescriptorException {
        ChannelDescriptor descriptor = ChannelDescriptor.open(runtime.getCurrentDirectory(), path, modes, runtime.getClassLoader());
        Stream stream = fdopen(runtime, descriptor, modes);

        try {
            if (modes.isAppendable()) stream.lseek(0, Stream.SEEK_END);
        } catch (PipeException pe) {
            // ignore; it's a pipe or fifo
        }

        return stream;
    }

    public Channel getChannel() {
        return getDescriptor().getChannel();
    }

    private static final class InputStreamAdapter extends java.io.InputStream {
        private final ChannelStream stream;

        public InputStreamAdapter(ChannelStream stream) {
            this.stream = stream;
        }

        @Override
        public int read() throws IOException {
            synchronized (stream) {
                // If it can be pulled direct from the buffer, don't go via the slow path
                if (stream.hasBufferedInputBytes()) {
                    try {
                        return stream.read();
                    } catch (BadDescriptorException ex) {
                        throw new IOException(ex.getMessage());
                    }
                }
            }

            byte[] b = new byte[1];
            // java.io.InputStream#read must return an unsigned value;
            return read(b, 0, 1) == 1 ? b[0] & 0xff: -1;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (bytes == null) {
                throw new NullPointerException("null destination buffer");
            }
            if ((len | off | (off + len) | (bytes.length - (off + len))) < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }

            try {
                synchronized(stream) {
                    final int available = stream.bufferedInputBytesRemaining();
                     if (available >= len) {
                        return stream.copyBufferedBytes(bytes, off, len);
                    } else if (stream.getDescriptor().getChannel() instanceof SelectableChannel) {
                        SelectableChannel ch = (SelectableChannel) stream.getDescriptor().getChannel();
                        synchronized (ch.blockingLock()) {
                            boolean oldBlocking = ch.isBlocking();
                            try {
                                if (!oldBlocking) {
                                    ch.configureBlocking(true);
                                }
                                return stream.bufferedRead(ByteBuffer.wrap(bytes, off, len), true);
                            } finally {
                                if (!oldBlocking) {
                                    ch.configureBlocking(oldBlocking);
                                }
                            }
                        }
                    } else {
                        return stream.bufferedRead(ByteBuffer.wrap(bytes, off, len), true);
                    }
                }
            } catch (BadDescriptorException ex) {
                throw new IOException(ex.getMessage());
            } catch (EOFException ex) {
                return -1;
            }
        }

        @Override
        public int available() throws IOException {
            synchronized (stream) {
                return !stream.eof ? stream.bufferedInputBytesRemaining() : 0;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                synchronized (stream) {
                    stream.fclose();
                }
            } catch (BadDescriptorException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    private static final class OutputStreamAdapter extends java.io.OutputStream {
        private final ChannelStream stream;

        public OutputStreamAdapter(ChannelStream stream) {
            this.stream = stream;
        }

        @Override
        public void write(int i) throws IOException {
            synchronized (stream) {
                if (!stream.isSync() && stream.hasBufferedOutputSpace()) {
                    stream.buffer.put((byte) i);
                    return;
                }
            }
            byte[] b = { (byte) i };
            write(b, 0, 1);
        }

        @Override
        public void write(byte[] bytes, int off, int len) throws IOException {
            if (bytes == null) {
                throw new NullPointerException("null source buffer");
            }
            if ((len | off | (off + len) | (bytes.length - (off + len))) < 0) {
                throw new IndexOutOfBoundsException();
            }

            try {
                synchronized(stream) {
                    if (!stream.isSync() && stream.bufferedOutputSpaceRemaining() >= len) {
                        stream.buffer.put(bytes, off, len);

                    } else if (stream.getDescriptor().getChannel() instanceof SelectableChannel) {
                        SelectableChannel ch = (SelectableChannel) stream.getDescriptor().getChannel();
                        synchronized (ch.blockingLock()) {
                            boolean oldBlocking = ch.isBlocking();
                            try {
                                if (!oldBlocking) {
                                    ch.configureBlocking(true);
                                }
                                stream.bufferedWrite(ByteBuffer.wrap(bytes, off, len));
                            } finally {
                                if (!oldBlocking) {
                                    ch.configureBlocking(oldBlocking);
                                }
                            }
                        }
                    } else {
                        stream.bufferedWrite(ByteBuffer.wrap(bytes, off, len));
                    }
                }
            } catch (BadDescriptorException ex) {
                throw new IOException(ex.getMessage());
            }
        }


        @Override
        public void close() throws IOException {
            try {
                synchronized (stream) {
                    stream.fclose();
                }
            } catch (BadDescriptorException ex) {
                throw new IOException(ex.getMessage());
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                synchronized (stream) {
                    stream.flushWrite(true);
                }
            } catch (BadDescriptorException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }
}
