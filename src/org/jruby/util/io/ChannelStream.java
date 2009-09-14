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

import static java.util.logging.Logger.getLogger;

import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

import org.jruby.Finalizable;
import org.jruby.Ruby;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyFile;

/**
 * <p>This file implements a seekable IO file.</p>
 */
public class ChannelStream implements Stream, Finalizable {
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
    private final static int BUFSIZE = 4 * 1024;
    
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
    
    private Ruby runtime;
    protected ModeFlags modes;
    protected boolean sync = false;
    
    protected volatile ByteBuffer buffer; // r/w buffer
    protected boolean reading; // are we reading or writing?
    private ChannelDescriptor descriptor;
    private boolean blocking = true;
    protected int ungotc = -1;
    private volatile boolean closedExplicitly = false;

    private volatile boolean eof = false;

    public ChannelStream(Ruby runtime, ChannelDescriptor descriptor, ModeFlags modes, FileDescriptor fileDescriptor) throws InvalidValueException {
        descriptor.checkNewModes(modes);
        
        this.runtime = runtime;
        this.descriptor = descriptor;
        this.modes = modes;
        this.buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.flip();
        this.reading = true;
        
        // this constructor is used by fdopen, so we don't increment descriptor ref count
    }

    public ChannelStream(Ruby runtime, ChannelDescriptor descriptor) {
        this(runtime, descriptor, descriptor.getFileDescriptor());
    }

    public ChannelStream(Ruby runtime, ChannelDescriptor descriptor, FileDescriptor fileDescriptor) {
        this.runtime = runtime;
        this.descriptor = descriptor;
        this.modes = descriptor.getOriginalModes();
        buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.flip();
        this.reading = true;
    }

    public ChannelStream(Ruby runtime, ChannelDescriptor descriptor, ModeFlags modes) throws InvalidValueException {
        descriptor.checkNewModes(modes);
        
        this.runtime = runtime;
        this.descriptor = descriptor;
        this.modes = modes;
        buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.flip();
        this.reading = true;
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
        return reading && buffer.hasRemaining();
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
        
        byte first = separator.bytes[separator.begin];

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
            for (int i = 0; i < separator.realSize; i++) {
                if (c == -1) {
                    break LineLoop;
                } else if (c != separator.bytes[separator.begin + i]) {
                    buf.append(c);
                    continue LineLoop;
                }
                buf.append(c);
                if (i < separator.realSize - 1) {
                    c = read();
                }
            }
            break;
        }

        if (separatorString == PARAGRAPH_DELIMETER) {
            while (c == separator.bytes[separator.begin]) {
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
            final long left = fileSize - channel.position() + bufferedBytesAvailable();
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
            ByteBuffer buf = ByteBuffer.wrap(result.unsafeBytes(), 
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
                tmp.limit(dst.remaining());
                dst.put(tmp);
            }
        }

        return bytesToCopy - dst.remaining();
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

        dst.ensure(Math.min(len, bufferedBytesAvailable()));

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
    private final int bufferedBytesAvailable() {
        return buffer.remaining() + (reading && ungotc != -1 ? 1 : 0);
    }

    /**
     * <p>Close IO handler resources.</p>
     * @throws IOException 
     * @throws BadDescriptorException 
     * 
     * @see org.jruby.util.IOHandler#close()
     */
    public synchronized void fclose() throws IOException, BadDescriptorException {
        closedExplicitly = true;
        close(false); // not closing from finalize
    }

    /**
     * Internal close, to safely work for finalizing.
     * @param finalizing true if this is in a finalizing context
     * @throws IOException
     * @throws BadDescriptorException
     */
    private void close(boolean finalizing) throws IOException, BadDescriptorException {
        try {
            flushWrite();

            descriptor.close();
            buffer = EMPTY_BUFFER;

            if (DEBUG) getLogger("ChannelStream").info("Descriptor for fileno "
                    + descriptor.getFileno() + " closed by stream");
        } finally {
            Ruby localRuntime = getRuntime();
            if (!finalizing && localRuntime != null) localRuntime.removeInternalFinalizer(this);
            
            // clear runtime so it doesn't get stuck in memory (JRUBY-2933)
            runtime = null;
        }
    }

    /**
     * Internal close, to safely work for finalizing.
     * @param finalizing true if this is in a finalizing context
     * @throws IOException 
     * @throws BadDescriptorException
     */
    private void closeForFinalize() {
        try {
            close(true);
        } catch (BadDescriptorException ex) {
            // silence
        } catch (IOException ex) {
            // silence
        }
    }

    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#flush()
     */
    public synchronized int fflush() throws IOException, BadDescriptorException {
        checkWritable();
        try {
            flushWrite();
        } catch (EOFException eof) {
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

    /**
     * @see org.jruby.util.IOHandler#getInputStream()
     */
    public InputStream newInputStream() {
        InputStream in = descriptor.getBaseInputStream();
        if (in == null) {
            return Channels.newInputStream((ReadableByteChannel)descriptor.getChannel());
        } else {
            return in;
        }
    }

    /**
     * @see org.jruby.util.IOHandler#getOutputStream()
     */
    public OutputStream newOutputStream() {
        return Channels.newOutputStream((WritableByteChannel)descriptor.getChannel());
    }
    
    public void clearerr() {
        eof = false;
    }
    
    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#isEOF()
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
     * @see org.jruby.util.IOHandler#pos()
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
     * @see org.jruby.util.IOHandler#seek(long, int)
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
                ioe.printStackTrace();
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

    /**
     * @see org.jruby.util.IOHandler#sync()
     */
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
            resultSize = (int) Math.min(fileChannel.size() - fileChannel.position() + bufferedBytesAvailable(), number);
        } else {
            //
            // Cannot discern the total read length - allocate at least enough for the buffered data
            //
            resultSize = Math.min(bufferedBytesAvailable(), number);
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
                    tmpDst.limit(bytesToRead);
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
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    private int bufferedWrite(ByteList buf) throws IOException, BadDescriptorException {
        checkWritable();
        ensureWrite();
        
        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) return 0;
        
        if (buf.length() > buffer.capacity()) { // Doesn't fit in buffer. Write immediately.
            flushWrite(); // ensure nothing left to write
            

            int n = descriptor.write(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
            if(n != buf.length()) {
                // TODO: check the return value here
            }
        } else {
            if (buf.length() > buffer.remaining()) flushWrite();
            
            buffer.put(buf.unsafeBytes(), buf.begin(), buf.length());
        }
        
        if (isSync()) flushWrite();
        
        return buf.realSize;
    }
    
    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#syswrite(String buf)
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
     * Ensure close (especially flush) when we're finished with
     */
    @Override
    public synchronized void finalize() {
        if (closedExplicitly) return;

        // FIXME: I got a bunch of NPEs when I didn't check for nulls here...HOW?!
        if (descriptor != null && descriptor.isSeekable() && descriptor.isOpen()) {
            closeForFinalize(); // close without removing from finalizers
        }
    }

    public int ready() throws IOException {
        if (descriptor.getChannel() instanceof SelectableChannel) {
            int ready_stat = 0;
            java.nio.channels.Selector sel = java.nio.channels.Selector.open();
            SelectableChannel selchan = (SelectableChannel)descriptor.getChannel();
            synchronized (selchan.blockingLock()) {
                boolean is_block = selchan.isBlocking();
                try {
                    selchan.configureBlocking(false);
                    selchan.register(sel, java.nio.channels.SelectionKey.OP_READ);
                    ready_stat = sel.selectNow();
                    sel.close();
                } catch (Throwable ex) {
                    ex.printStackTrace();
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
                    return descriptor.write(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
                } finally {
                    if (oldBlocking) {
                        selectableChannel.configureBlocking(oldBlocking);
                    }
                }
            }
        } else {
            return descriptor.write(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
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

        if (bufferedBytesAvailable() > 0) {
            // already have some bytes buffered, just return those
            return bufferedRead(Math.min(bufferedBytesAvailable(), number));
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
            descriptor = new ChannelDescriptor(new NullChannel(), descriptor.getFileno(), modes, new FileDescriptor());
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
            
            descriptor = new ChannelDescriptor(file.getChannel(), descriptor.getFileno(), modes, file.getFD());
        
            if (modes.isAppendable()) lseek(0, SEEK_END);
        }
    }
    
    public static Stream fopen(Ruby runtime, String path, ModeFlags modes) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException, InvalidValueException, PipeException, BadDescriptorException {
        String cwd = runtime.getCurrentDirectory();
        
        ChannelDescriptor descriptor = ChannelDescriptor.open(cwd, path, modes);
        
        Stream stream = fdopen(runtime, descriptor, modes);
        
        if (modes.isAppendable()) stream.lseek(0, Stream.SEEK_END);
        
        return stream;
    }
    
    public static Stream fdopen(Ruby runtime, ChannelDescriptor descriptor, ModeFlags modes) throws InvalidValueException {
        Stream handler = new ChannelStream(runtime, descriptor, modes, descriptor.getFileDescriptor());
        
        return handler;
    }
}
