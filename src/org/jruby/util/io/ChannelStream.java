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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import static java.util.logging.Logger.getLogger;
import org.jruby.Finalizable;
import org.jruby.Ruby;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyFile;

/**
 * <p>This file implements a seekable IO file.</p>
 */
public class ChannelStream implements Stream, Finalizable {
    private final static boolean DEBUG = false;
    private final static int BUFSIZE = 16;
    
    private Ruby runtime;
    protected ModeFlags modes;
    protected boolean sync = false;
    
    protected ByteBuffer buffer; // r/w buffer
    protected boolean reading; // are we reading or writing?
    private ChannelDescriptor descriptor;
    private boolean blocking = true;
    protected int ungotc = -1;

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
                buffer.clear();
                int read = ((ReadableByteChannel)descriptor.getChannel()).read(buffer);
                buffer.flip();
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
    
    public synchronized ByteList readall() throws IOException, BadDescriptorException {
        if (descriptor.isSeekable()) {
            invalidateBuffer();
            FileChannel channel = (FileChannel)descriptor.getChannel();
            long left = channel.size() - channel.position();
            if (left == 0) {
                eof = true;
                return null;
            }

            return fread((int) left);
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
     * <p>Close IO handler resources.</p>
     * @throws IOException 
     * @throws BadDescriptorException 
     * 
     * @see org.jruby.util.IOHandler#close()
     */
    public synchronized void fclose() throws IOException, BadDescriptorException {
        close(false); // not closing from finalise
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

            if (DEBUG) getLogger("ChannelStream").info("Descriptor for fileno " + descriptor.getFileno() + " closed by stream");
        } finally {
            if (!finalizing) getRuntime().removeInternalFinalizer(this);
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
    private void flushWrite() throws IOException {
        if (reading || !modes.isWritable() || buffer.position() == 0) return; // Don't bother
            
        buffer.flip();
        ((WritableByteChannel)descriptor.getChannel()).write(buffer);
        buffer.clear();
    }

    /**
     * @see org.jruby.util.IOHandler#getInputStream()
     */
    public InputStream newInputStream() {
        return new BufferedInputStream(Channels.newInputStream((ReadableByteChannel)descriptor.getChannel()));
    }

    /**
     * @see org.jruby.util.IOHandler#getOutputStream()
     */
    public OutputStream newOutputStream() {
        return new BufferedOutputStream(Channels.newOutputStream((WritableByteChannel)descriptor.getChannel()));
    }
    
    private boolean eof = false;
    
    public void clearerr() {
        eof = false;
    }
    
    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#isEOF()
     */
    public synchronized boolean feof() throws IOException, BadDescriptorException {
        checkReadable();
        
        if (eof) {
            return true;
        } else {
            return false;
        }
//        
//        if (reading && buffer.hasRemaining()) return false;
//        
//        if (descriptor.isSeekable()) {
//            FileChannel fileChannel = (FileChannel)descriptor.getChannel();
//            return (fileChannel.size() == fileChannel.position());
//        } else if (descriptor.getChannel() instanceof SocketChannel) {
//            return false;
//        } else {
//            checkReadable();
//            ensureRead();
//
//            if (ungotc > 0) {
//                return false;
//            }
//            // TODO: this is new to replace what's below
//            ungotc = read();
//            if (ungotc == -1) {
//                eof = true;
//                return true;
//            }
//            // FIXME: this was here before; need a better way?
////            if (fillInBuffer() < 0) {
////                return true;
////            }
//            return false;
//        }
    }
    
    /**
     * @throws IOException 
     * @see org.jruby.util.IOHandler#pos()
     */
    public synchronized long fgetpos() throws IOException {
        // Correct position for read / write buffering (we could invalidate, but expensive)
        int offset = (reading) ? - buffer.remaining() : buffer.position();
        FileChannel fileChannel = (FileChannel)descriptor.getChannel();
        return fileChannel.position() + offset;
    }
    
    /**
     * @throws IOException 
     * @throws InvalidValueException 
     * @see org.jruby.util.IOHandler#seek(long, int)
     */
    public synchronized void fseek(long offset, int type) throws IOException, InvalidValueException, PipeException {
        if (descriptor.getChannel() instanceof FileChannel) {
            FileChannel fileChannel = (FileChannel)descriptor.getChannel();
            if (reading) {
                if (buffer.remaining() > 0) {
                    fileChannel.position(fileChannel.position() - buffer.remaining());
                }
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
                    fileChannel.position(fileChannel.position() + offset);
                    break;
                case SEEK_END:
                    fileChannel.position(fileChannel.size() + offset);
                    break;
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException();
            }
        } else {
            throw new PipeException();
        }
    }

    /**
     * @see org.jruby.util.IOHandler#sync()
     */
    public void sync() throws IOException {
        flushWrite();
    }

    /**
     * Ensure buffer is ready for reading, flushing remaining writes if required
     * @throws IOException
     */
    private void ensureRead() throws IOException {
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
    private void ensureReadNonBuffered() throws IOException {
        if (reading) {
            if (buffer.hasRemaining()) {
                throw getRuntime().newIOError("sysread for buffered IO");
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
        
        return byteList;
    }

    private ByteList bufferedRead(int number) throws IOException {
        checkReadable();
        ensureRead();
        
        ByteList result = new ByteList();
        int len = -1;
        if (buffer.hasRemaining()) { // already have some bytes buffered
            len = (number <= buffer.remaining()) ? number : buffer.remaining();
            result.append(buffer, len);
        }
        
        ReadableByteChannel readChannel = (ReadableByteChannel)descriptor.getChannel();
        int read = BUFSIZE;
        while (read == BUFSIZE && result.length() != number) { // not complete. try to read more
            buffer.clear(); 
            read = readChannel.read(buffer);
            buffer.flip();
            if (read == -1) break;
            int desired = number - result.length();
            len = (desired < read) ? desired : read;
            result.append(buffer, len);
        }
        
        if (result.length() == 0 && number != 0) throw new java.io.EOFException();
        return result;
    }
    
    private int bufferedRead() throws IOException, BadDescriptorException {
        ensureRead();
        
        if (!buffer.hasRemaining()) {
            buffer.clear();
            int read = descriptor.read(buffer);
            buffer.flip();
            
            if (read <= 0) {
                eof = true;
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
        getRuntime().secure(4);
        checkWritable();
        ensureWrite();
        
        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) return 0;
        
        if (buf.length() > buffer.capacity()) { // Doesn't fit in buffer. Write immediately.
            flushWrite(); // ensure nothing left to write
            
            ((WritableByteChannel)descriptor.getChannel()).write(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
        } else {
            if (buf.length() > buffer.remaining()) flushWrite();
            
            buffer.put(buf.unsafeBytes(), buf.begin(), buf.length());
        }
        
        if (isSync()) sync();
        
        return buf.realSize;
    }
    
    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    private int bufferedWrite(int c) throws IOException, BadDescriptorException {
        getRuntime().secure(4);
        checkWritable();
        ensureWrite();

        if (!buffer.hasRemaining()) flushWrite();
        
        buffer.put((byte) c);
            
        if (isSync()) sync();
            
        return 1;
    }
    
    public synchronized void ftruncate(long newLength) throws IOException {
        invalidateBuffer();
        FileChannel fileChannel = (FileChannel)descriptor.getChannel();
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
    private void invalidateBuffer() throws IOException {
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
    public void finalize() {
        // FIXME: I got a bunch of NPEs when I didn't check for nulls here...HOW?!
        if (descriptor != null && descriptor.isSeekable() && descriptor.isOpen()) closeForFinalize(); // close without removing from finalizers
    }
    
    public int ready() throws IOException {
        return newInputStream().available();
    }

    public synchronized void fputc(int c) throws IOException, BadDescriptorException {
        try {
            bufferedWrite(c);
            fflush();
        } catch (IOException e) {
        }
    }

    public void ungetc(int c) {
        // Ruby silently ignores negative ints for some reason?
        if (c >= 0) {
            ungotc = c;
        }
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

    public synchronized ByteList fread(int number) throws IOException, BadDescriptorException {
        try {
            if (number == 0) {
                if (eof) {
                    return null;
                } else {
                    return new ByteList(0);
                }
            }

            if (ungotc >= 0) {
                ByteList buf2 = bufferedRead(number - 1);
                buf2.prepend((byte)ungotc);
                ungotc = -1;
                return buf2;
            }

            return bufferedRead(number);
        } catch (EOFException e) {
            eof = true;
            return null;
        }
    }

    public synchronized ByteList readpartial(int number) throws IOException, BadDescriptorException, EOFException {
        if (descriptor.getChannel() instanceof SelectableChannel) {
            if (ungotc >= 0) {
                ByteList buf2 = bufferedRead(number - 1);
                buf2.prepend((byte)ungotc);
                ungotc = -1;
                return buf2;
            } else {
                return bufferedRead(number);
            }
        } else {
            return null;
        }
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

    public synchronized void freopen(String path, ModeFlags modes) throws DirectoryAsFileException, IOException, InvalidValueException, PipeException, BadDescriptorException {
        // flush first
        flushWrite();
        
        this.modes = modes;
        String cwd = getRuntime().getCurrentDirectory();
        JRubyFile theFile = JRubyFile.create(cwd,path);

        if (theFile.isDirectory() && modes.isWritable()) throw new DirectoryAsFileException();
        
        if (modes.isCreate()) {
            if (theFile.exists() && modes.isExclusive()) {
                throw getRuntime().newErrnoEEXISTError("File exists - " + path);
            }
            theFile.createNewFile();
        } else {
            if (!theFile.exists()) {
                throw getRuntime().newErrnoENOENTError("file not found - " + path);
            }
        }
        
        if (descriptor.isOpen()) {
            descriptor.close();
        }

        // We always open this rw since we can only open it r or rw.
        RandomAccessFile file = new RandomAccessFile(theFile, modes.toJavaModeString());

        if (modes.isTruncate()) file.setLength(0L);

        descriptor = new ChannelDescriptor(file.getChannel(), descriptor.getFileno(), modes, file.getFD());
        
        if (modes.isAppendable()) fseek(0, SEEK_END);
    }
    
    public static Stream fopen(Ruby runtime, String path, ModeFlags modes) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException, InvalidValueException, PipeException {
        String cwd = runtime.getCurrentDirectory();
        
        ChannelDescriptor descriptor = ChannelDescriptor.open(cwd, path, modes, -1);
        
        Stream stream = fdopen(runtime, descriptor, modes);
        
        if (modes.isAppendable()) stream.fseek(0, Stream.SEEK_END);
        
        return stream;
    }
    
    public static Stream fdopen(Ruby runtime, ChannelDescriptor descriptor, ModeFlags modes) throws InvalidValueException {
        Stream handler = new ChannelStream(runtime, descriptor, modes, descriptor.getFileDescriptor());
        
        return handler;
    }
}
