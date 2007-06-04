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
package org.jruby.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jruby.Finalizable;
import org.jruby.Ruby;
import org.jruby.RubyIO;

/**
 * <p>This file implements a seekable IO file.</p>
 * 
 * @author Thomas E Enebo (enebo@acm.org)
 */
public class IOHandlerSeekable extends IOHandlerJavaIO implements Finalizable {
    private final static int BUFSIZE = 1024;
    
    protected RandomAccessFile file;
    protected String path;
    protected String cwd;
    protected ByteBuffer buffer; // r/w buffer
    protected boolean reading; // are we reading or writing?
    protected FileChannel channel;
    
    public IOHandlerSeekable(Ruby runtime, String path, IOModes modes) 
        throws IOException, InvalidValueException {
        super(runtime);
        
        this.path = path;
        this.modes = modes;
        this.cwd = runtime.getCurrentDirectory();
        JRubyFile theFile = JRubyFile.create(cwd,path);
        
        if(!theFile.exists()) {
            if (modes.isReadable() && !modes.isWriteable()) {
                throw new FileNotFoundException();
            }
        }

        // Do not open as 'rw' if we don't need to since a file with permissions for read-only
        // will barf if opened 'rw'.
        String javaMode = "r";
        if (modes.isWriteable()) {
            javaMode += "w";
        }
        
        // We always open this rw since we can only open it r or rw.
        file = new RandomAccessFile(theFile, javaMode);
        if (modes.shouldTruncate()) {
            file.setLength(0L);
        }
        channel = file.getChannel();
        isOpen = true;
        buffer = ByteBuffer.allocate(BUFSIZE);
        buffer.flip();
        reading = true;
        
        if (modes.isAppendable()) {
            seek(0, SEEK_END);
        }

        // We give a fileno last so that we do not consume these when
        // we have a problem opening a file.
        fileno = RubyIO.getNewFileno();
        
        // Ensure we clean up after ourselves ... eventually
        runtime.addFinalizer(this);
    }

    private void reopen() throws IOException {
        long pos = pos();

        String javaMode = "r";
        if (modes.isWriteable()) {
            javaMode += "w";
        }
        
        JRubyFile theFile = JRubyFile.create(cwd,path);
        file.close();
        file = new RandomAccessFile(theFile, javaMode);
        channel = file.getChannel();
        isOpen = true;
        buffer.clear();
        buffer.flip();
        reading = true;
        
        try {
            seek(pos,SEEK_SET);
        } catch(Exception e) {
            throw new IOException();
        }
    }

    private void checkReopen() throws IOException {
        if(file.length() != new java.io.File(path).length()) {
            reopen();
        }
    }
    
    public ByteList getsEntireStream() throws IOException {
        checkReopen();
        invalidateBuffer();
        long left = channel.size() - channel.position();
        if (left == 0) return null;
        
        try {
        // let's hope no one grabs big files...
        return sysread((int)left);
        } catch (BadDescriptorException e) {
            throw new IOException(e.getMessage()); // Ugh! But why rewrite the same code?
        }
    }


    public IOHandler cloneIOHandler() throws IOException, PipeException, InvalidValueException {
        IOHandler newHandler = new IOHandlerSeekable(getRuntime(), path, modes); 
            
        newHandler.seek(pos(), SEEK_CUR);
            
        return newHandler;
    }
    
    /**
     * <p>Close IO handler resources.</p>
     * @throws IOException 
     * @throws BadDescriptorException 
     * 
     * @see org.jruby.util.IOHandler#close()
     */
    public void close() throws IOException, BadDescriptorException {
        close(false); // not closing from finalise
    }
    
    /**
     * Internal close, to safely work for finalizing.
     * @param finalizing true if this is in a finalizing context
     * @throws IOException 
     * @throws BadDescriptorException
     */
    private void close(boolean finalizing) throws IOException, BadDescriptorException {
        if (!isOpen()) {
            throw new BadDescriptorException();
        }
        
        isOpen = false;
        flushWrite();
        channel.close();
        file.close();
        if (!finalizing) getRuntime().removeFinalizer(this);
    }

    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#flush()
     */
    public void flush() throws IOException, BadDescriptorException {
        checkWriteable();
        flushWrite();
    }
    
    /**
     * Flush the write buffer to the channel (if needed)
     * @throws IOException
     */
    private void flushWrite() throws IOException {
        if (reading || !modes.isWritable() || buffer.position() == 0) // Don't bother
            return;
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
    }

    /**
     * @see org.jruby.util.IOHandler#getInputStream()
     */
    public InputStream getInputStream() {
        return new BufferedInputStream(new DataInputBridgeStream(file));
    }

    /**
     * @see org.jruby.util.IOHandler#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return new BufferedOutputStream(new DataOutputBridgeStream(file));
    }
    
    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#isEOF()
     */
    public boolean isEOF() throws IOException, BadDescriptorException {
        checkReadable();
        
        if (reading && buffer.hasRemaining()) return false;
        
        return (channel.size() == channel.position());
    }
    
    /**
     * @see org.jruby.util.IOHandler#pid()
     */
    public int pid() {
        // A file is not a process.
        return -1;
    }
    
    /**
     * @throws IOException 
     * @see org.jruby.util.IOHandler#pos()
     */
    public long pos() throws IOException {
        checkOpen();
        // Correct position for read / write buffering (we could invalidate, but expensive)
        int offset = (reading) ? - buffer.remaining() : buffer.position();
        return channel.position() + offset;
    }
    
    public void resetByModes(IOModes newModes) throws IOException, InvalidValueException {
        if (newModes.isAppendable()) {
            seek(0L, SEEK_END);
        } else if (newModes.isWriteable()) {
            rewind();
        }
    }

    /**
     * @throws IOException 
     * @throws InvalidValueException 
     * @see org.jruby.util.IOHandler#rewind()
     */
    public void rewind() throws IOException, InvalidValueException {
        seek(0, SEEK_SET);
    }
    
    /**
     * @throws IOException 
     * @throws InvalidValueException 
     * @see org.jruby.util.IOHandler#seek(long, int)
     */
    public void seek(long offset, int type) throws IOException, InvalidValueException {
        checkOpen();
        invalidateBuffer();
        try {
            switch (type) {
            case SEEK_SET:
                channel.position(offset);
                break;
            case SEEK_CUR:
                channel.position(channel.position() + offset);
                break;
            case SEEK_END:
                channel.position(channel.size() + offset);
                break;
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException();
        }
    }

    /**
     * @see org.jruby.util.IOHandler#sync()
     */
    public void sync() throws IOException {
        flushWrite();
        channel.force(false);
    }

    public ByteList sysread(int number) throws IOException, BadDescriptorException {
        if (!isOpen()) {
            throw new IOException("File not open");
        }
        checkReadable();
        ensureRead();
        
        ByteBuffer buf = ByteBuffer.allocate(number);
        if (buffer.hasRemaining()) {// already have some bytes buffered
            putInto(buf, buffer);
        }
        
        if (buf.position() != buf.capacity()) { // not complete. try to read more
            if (buf.capacity() > buffer.capacity()) // big read. just do it.
                channel.read(buf);
            else { // buffer it
                buffer.clear();
                channel.read(buffer);
                buffer.flip();
                putInto(buf, buffer); // get what we need
            }
        }
        
        if (buf.position() == 0) throw new java.io.EOFException();
        return new ByteList(buf.array(),0,buf.position(),false);
    }

    /**
     * Put one buffer into another, truncating the put (rather than throwing an exception)
     * if src doesn't fit into dest. Shame this doesn't exist already.
     * @param dest The destination buffer which will receive bytes
     * @param src The buffer to read bytes from
     */
    private static void putInto(ByteBuffer dest, ByteBuffer src) {
        int destAvail = dest.capacity() - dest.position();
        if (src.remaining() > destAvail) { // already have more than enough bytes available
            // ByteBuffer seems to be missing a useful method here
            int oldLimit = src.limit();
            src.limit(src.position() + destAvail);
            dest.put(src);
            src.limit(oldLimit);
        } else {
            dest.put(src);
        }
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
     * Ensure buffer is ready for writing.
     * @throws IOException
     */
    private void ensureWrite() throws IOException {
        if (!reading) return;
        if (buffer.hasRemaining()) // we have read ahead, and need to back up
            channel.position(channel.position() - buffer.remaining());
        buffer.clear();
        reading = false;
    }
    
    /**
     * @see org.jruby.util.IOHandler#sysread()
     */
    public int sysread() throws IOException {
        ensureRead();
        
        if (!buffer.hasRemaining()) {
            buffer.clear();
            int read = channel.read(buffer);
            buffer.flip();
            if (read == -1) return -1;
        }
        return buffer.get();
    }
    
    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    public int syswrite(ByteList buf) throws IOException, BadDescriptorException {
        getRuntime().secure(4);
        checkWriteable();
        ensureWrite();
        
        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) {
            return 0;
        }
        
        if (buf.length() > buffer.capacity()) { // Doesn't fit in buffer. Write immediately.
            flushWrite(); // ensure nothing left to write
            channel.write(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
        }
        else {
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
    public int syswrite(int c) throws IOException, BadDescriptorException {
        getRuntime().secure(4);
        checkWriteable();
        ensureWrite();

        if (!buffer.hasRemaining()) flushWrite();
        
        buffer.put((byte) c);
            
        if (isSync()) sync();
            
        return 1;
    }
    
    public void truncate(long newLength) throws IOException {
        invalidateBuffer();
        file.setLength(newLength);
    }
    
    public FileChannel getFileChannel() {
        return channel;
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
            if (posOverrun != 0) channel.position(channel.position() - posOverrun);
        }
    }
    
    /**
     * Ensure close (especially flush) when we're finished with
     */
    public void finalize() {
        try {
            if (isOpen) close(true); // close without removing from finalizers
        } catch (Exception e) { // What else could we do?
            e.printStackTrace();
        }
    }
}
