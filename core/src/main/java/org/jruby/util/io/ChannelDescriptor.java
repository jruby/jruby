/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008 Charles O Nutter <headius@headius.com>
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
package org.jruby.util.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;

import org.jruby.Ruby;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyFile;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

/**
 * ChannelDescriptor provides an abstraction similar to the concept of a
 * "file descriptor" on any POSIX system. In our case, it's a numbered object
 * (fileno) enclosing a Channel (@see java.nio.channels.Channel), FileDescriptor
 * (@see java.io.FileDescriptor), and flags under which the original open occured
 * (@see org.jruby.util.io.ModeFlags). Several operations you would normally
 * expect to use with a POSIX file descriptor are implemented here and used by
 * higher-level classes to implement higher-level IO behavior.
 * 
 * Note that the channel specified when constructing a ChannelDescriptor will
 * be reference-counted; that is, until all known references to it through this
 * class have gone away, it will be left open. This is to support operations
 * like "dup" which must produce two independent ChannelDescriptor instances
 * that can be closed separately without affecting the other.
 * 
 * At present there's no way to simulate the behavior on some platforms where
 * POSIX dup also allows independent positioning information.
 */
@Deprecated
public class ChannelDescriptor {
    private static final Logger LOG = LoggerFactory.getLogger("ChannelDescriptor");

    /** Whether to log debugging information */
    private static final boolean DEBUG = false;
    
    /** The java.nio.channels.Channel this descriptor wraps. */
    private Channel channel;
    /**
     * The file number (equivalent to the int file descriptor value in POSIX)
     * for this descriptor. This is generated new for most ChannelDescriptor
     * instances, except when they need to masquerade as another fileno.
     */
    private int internalFileno;
    /** The java.io.FileDescriptor object for this descriptor. */
    private FileDescriptor fileDescriptor;
    /**
     * The original org.jruby.util.io.ModeFlags with which the specified
     * channel was opened.
     */
    private ModeFlags originalModes;
    /** 
     * The reference count for the provided channel.
     * Only counts references through ChannelDescriptor instances.
     */
    private AtomicInteger refCounter;

    /**
     * Used to work-around blocking problems with STDIN. In most cases <code>null</code>.
     * See {@link ChannelDescriptor#ChannelDescriptor(java.io.InputStream, ModeFlags, java.io.FileDescriptor)}
     * for more details. You probably should not use it.
     */
    private InputStream baseInputStream;
    
    /**
     * Process streams get Channel.newChannel()ed into FileChannel but are not actually
     * seekable. So instead of just the isSeekable check doing instanceof FileChannel,
     * we must also add this boolean to check, which we set to false when it's known
     * that the incoming channel is from a process.
     * 
     * FIXME: This is gross, and it's NIO's fault for not providing a nice way to
     * tell if a channel is "really" seekable.
     */
    private boolean canBeSeekable = true;
    
    /**
     * If the incoming channel is already in append mode (i.e. it will do the
     * requisite seeking), we don't want to do our own additional seeks.
     */
    private boolean isInAppendMode = false;
    
    /**
     * Whether the current channe is writable or not.
     */
    private boolean readableChannel;
    
    /**
     * Whether the current channel is readable or not.
     */
    private boolean writableChannel;
    
    /**
     * Whether the current channel is seekable or not.
     */
    private boolean seekableChannel;
    
    /**
     * Construct a new ChannelDescriptor with the specified channel, file number,
     * mode flags, file descriptor object and reference counter. This constructor
     * is only used when constructing a new copy of an existing ChannelDescriptor
     * with an existing reference count, to allow the two instances to safely
     * share and appropriately close  a given channel.
     * 
     * @param channel The channel for the new descriptor, which will be shared with another
     * @param fileno The new file number for the new descriptor
     * @param originalModes The mode flags to use as the "origina" set for this descriptor
     * @param fileDescriptor The java.io.FileDescriptor object to associate with this ChannelDescriptor
     * @param refCounter The reference counter from another ChannelDescriptor being duped.
     * @param canBeSeekable If the underlying channel can be considered seekable.
     * @param isInAppendMode If the underlying channel is already in append mode.
     */
    private ChannelDescriptor(Channel channel, int fileno, ModeFlags originalModes, FileDescriptor fileDescriptor, AtomicInteger refCounter, boolean canBeSeekable, boolean isInAppendMode) {
        this.refCounter = refCounter;
        this.channel = channel;
        this.internalFileno = fileno;
        this.originalModes = originalModes;
        this.fileDescriptor = fileDescriptor;
        this.canBeSeekable = canBeSeekable;
        this.isInAppendMode = isInAppendMode;
        
        this.readableChannel = channel instanceof ReadableByteChannel;
        this.writableChannel = channel instanceof WritableByteChannel;
        this.seekableChannel = channel instanceof FileChannel;

        registerDescriptor(this);
    }

    private ChannelDescriptor(Channel channel, int fileno, ModeFlags originalModes, FileDescriptor fileDescriptor) {
        this(channel, fileno, originalModes, fileDescriptor, new AtomicInteger(1), true, false);
    }

    /**
     * Construct a new ChannelDescriptor with the given channel, file number, mode flags,
     * and file descriptor object. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed.
     * 
     * @param channel The channel for the new descriptor
     * @param originalModes The mode flags for the new descriptor
     * @param fileDescriptor The java.io.FileDescriptor object for the new descriptor
     */
    public ChannelDescriptor(Channel channel, ModeFlags originalModes, FileDescriptor fileDescriptor) {
        this(channel, getNewFileno(), originalModes, fileDescriptor, new AtomicInteger(1), true, false);
    }

    /**
     * Construct a new ChannelDescriptor with the given channel, file number, mode flags,
     * and file descriptor object. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed.
     * 
     * @param channel The channel for the new descriptor
     * @param originalModes The mode flags for the new descriptor
     * @param fileDescriptor The java.io.FileDescriptor object for the new descriptor
     */
    public ChannelDescriptor(Channel channel, ModeFlags originalModes, FileDescriptor fileDescriptor, boolean isInAppendMode) {
        this(channel, getNewFileno(), originalModes, fileDescriptor, new AtomicInteger(1), true, isInAppendMode);
    }

    /**
     * Construct a new ChannelDescriptor with the given channel, file number, mode flags,
     * and file descriptor object. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed.
     *
     * @param channel The channel for the new descriptor
     * @param originalModes The mode flags for the new descriptor
     */
    public ChannelDescriptor(Channel channel, ModeFlags originalModes) {
        this(channel, getNewFileno(), originalModes, FilenoUtil.getDescriptorFromChannel(channel), new AtomicInteger(1), true, false);
    }

    /**
     * Special constructor to create the ChannelDescriptor out of the stream, file number,
     * mode flags, and file descriptor object. The channel will be created from the
     * provided stream. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed. <b>Note:</b> in most cases, you should not
     * use this constructor, it's reserved mostly for STDIN.
     *
     * @param baseInputStream The stream to create the channel for the new descriptor
     * @param originalModes The mode flags for the new descriptor
     * @param fileDescriptor The java.io.FileDescriptor object for the new descriptor
     */
    public ChannelDescriptor(InputStream baseInputStream, ModeFlags originalModes, FileDescriptor fileDescriptor) {
        // The reason why we need the stream is to be able to invoke available() on it.
        // STDIN in Java is non-interruptible, non-selectable, and attempt to read
        // on such stream might lead to thread being blocked without *any* way to unblock it.
        // That's where available() comes it, so at least we could check whether
        // anything is available to be read without blocking.
        this(Channels.newChannel(baseInputStream), getNewFileno(), originalModes, fileDescriptor, new AtomicInteger(1), true, false);
        this.baseInputStream = baseInputStream;
    }

    /**
     * Special constructor to create the ChannelDescriptor out of the stream, file number,
     * mode flags, and file descriptor object. The channel will be created from the
     * provided stream. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed. <b>Note:</b> in most cases, you should not
     * use this constructor, it's reserved mostly for STDIN.
     *
     * @param baseInputStream The stream to create the channel for the new descriptor
     * @param originalModes The mode flags for the new descriptor
     */
    public ChannelDescriptor(InputStream baseInputStream, ModeFlags originalModes) {
        // The reason why we need the stream is to be able to invoke available() on it.
        // STDIN in Java is non-interruptible, non-selectable, and attempt to read
        // on such stream might lead to thread being blocked without *any* way to unblock it.
        // That's where available() comes it, so at least we could check whether
        // anything is available to be read without blocking.
        this(Channels.newChannel(baseInputStream), getNewFileno(), originalModes, new FileDescriptor(), new AtomicInteger(1), true, false);
        this.baseInputStream = baseInputStream;
    }

    /**
     * Construct a new ChannelDescriptor with the given channel, file number,
     * and file descriptor object. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed. The channel's capabilities will be used
     * to determine the "original" set of mode flags.
     * 
     * @param channel The channel for the new descriptor
     * @param fileDescriptor The java.io.FileDescriptor object for the new descriptor
     */
    public ChannelDescriptor(Channel channel, FileDescriptor fileDescriptor) throws InvalidValueException {
        this(channel, ModeFlags.getModesFromChannel(channel), fileDescriptor);
    }
    
    @Deprecated
    public ChannelDescriptor(Channel channel, int fileno, FileDescriptor fileDescriptor) throws InvalidValueException {
        this(channel, ModeFlags.getModesFromChannel(channel), fileDescriptor);
    }

    /**
     * Construct a new ChannelDescriptor with the given channel, file number,
     * and file descriptor object. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed. The channel's capabilities will be used
     * to determine the "original" set of mode flags. This version generates a
     * new fileno.
     *
     * @param channel The channel for the new descriptor
     */
    public ChannelDescriptor(Channel channel) throws InvalidValueException {
        this(channel, ModeFlags.getModesFromChannel(channel), FilenoUtil.getDescriptorFromChannel(channel));
    }

    /**
     * Get this descriptor's file number.
     * 
     * @return the fileno for this descriptor
     */
    public int getFileno() {
        return internalFileno;
    }
    
    /**
     * Get the FileDescriptor object associated with this descriptor. This is
     * not guaranteed to be a "valid" descriptor in the terms of the Java
     * implementation, but is provided for completeness and for cases where it
     * is possible to get a valid FileDescriptor for a given channel.
     * 
     * @return the java.io.FileDescriptor object associated with this descriptor
     */
    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    /**
     * The channel associated with this descriptor. The channel will be reference
     * counted through ChannelDescriptor and kept open until all ChannelDescriptor
     * objects have been closed. References that leave ChannelDescriptor through
     * this method will not be counted.
     * 
     * @return the java.nio.channels.Channel associated with this descriptor
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * This is intentionally non-public, since it should not be really
     * used outside of very limited use case (handling of STDIN).
     * See {@link ChannelDescriptor#ChannelDescriptor(java.io.InputStream, ModeFlags, java.io.FileDescriptor)}
     * for more info.
     */
    /*package-protected*/ InputStream getBaseInputStream() {
        return baseInputStream;
    }

    /**
     * Whether the channel associated with this descriptor is seekable (i.e.
     * whether it is instanceof FileChannel).
     * 
     * @return true if the associated channel is seekable, false otherwise
     */
    public boolean isSeekable() {
        return canBeSeekable && seekableChannel;
    }
    
    /**
     * Set the channel to be explicitly seekable or not, for streams that appear
     * to be seekable with the instanceof FileChannel check.
     * 
     * @param canBeSeekable Whether the channel is seekable or not.
     */
    public void setCanBeSeekable(boolean canBeSeekable) {
        this.canBeSeekable = canBeSeekable;
    }
    
    /**
     * Whether the channel associated with this descriptor is a NullChannel,
     * for which many operations are simply noops.
     */
    public boolean isNull() {
        return channel instanceof NullChannel;
    }

    /**
     * Whether the channel associated with this descriptor is writable (i.e.
     * whether it is instanceof WritableByteChannel).
     * 
     * @return true if the associated channel is writable, false otherwise
     */
    public boolean isWritable() {
        return writableChannel;
    }

    /**
     * Whether the channel associated with this descriptor is readable (i.e.
     * whether it is instanceof ReadableByteChannel).
     * 
     * @return true if the associated channel is readable, false otherwise
     */
    public boolean isReadable() {
        return readableChannel;
    }
    
    /**
     * Whether the channel associated with this descriptor is open.
     * 
     * @return true if the associated channel is open, false otherwise
     */
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    /**
     * Check whether the isOpen returns true, raising a BadDescriptorException if
     * it returns false.
     * 
     * @throws org.jruby.util.io.BadDescriptorException if isOpen returns false
     */
    public void checkOpen() throws BadDescriptorException {
        if (!isOpen()) {
            throw new BadDescriptorException();
        }
    }
    
    /**
     * Get the original mode flags for the descriptor.
     * 
     * @return the original mode flags for the descriptor
     */
    public ModeFlags getOriginalModes() {
        return originalModes;
    }
    
    /**
     * Check whether a specified set of mode flags is a superset of this
     * descriptor's original set of mode flags.
     * 
     * @param newModes The modes to confirm as superset
     * @throws org.jruby.util.io.InvalidValueException if the modes are not a superset
     */
    public void checkNewModes(ModeFlags newModes) throws InvalidValueException {
        if (!newModes.isSubsetOf(originalModes)) {
            throw new InvalidValueException();
        }
    }
    
    /**
     * Mimics the POSIX dup(2) function, returning a new descriptor that references
     * the same open channel.
     * 
     * @return A duplicate ChannelDescriptor based on this one
     */
    public ChannelDescriptor dup() {
        synchronized (refCounter) {
            refCounter.incrementAndGet();

            int newFileno = getNewFileno();
            
            if (DEBUG) LOG.info("Reopen fileno {}, refs now: {}", newFileno, refCounter.get());

            return new ChannelDescriptor(channel, newFileno, originalModes, fileDescriptor, refCounter, canBeSeekable, isInAppendMode);
        }
    }
    
    /**
     * Mimics the POSIX dup2(2) function, returning a new descriptor that references
     * the same open channel but with a specified fileno.
     * 
     * @param fileno The fileno to use for the new descriptor
     * @return A duplicate ChannelDescriptor based on this one
     */
    public ChannelDescriptor dup2(int fileno) {
        synchronized (refCounter) {
            refCounter.incrementAndGet();

            if (DEBUG) LOG.info("Reopen fileno {}, refs now: {}", fileno, refCounter.get());

            return new ChannelDescriptor(channel, fileno, originalModes, fileDescriptor, refCounter, canBeSeekable, isInAppendMode);
        }
    }
    
    /**
     * Mimics the POSIX dup2(2) function, returning a new descriptor that references
     * the same open channel but with a specified fileno. This differs from the fileno
     * version by making the target descriptor into a new reference to the current
     * descriptor's channel, closing what it originally pointed to and preserving
     * its original fileno.
     *
     * @param other the descriptor to dup this one into
     */
    public void dup2Into(ChannelDescriptor other) throws BadDescriptorException, IOException {
        synchronized (refCounter) {
            refCounter.incrementAndGet();

            if (DEBUG) LOG.info("Reopen fileno {}, refs now: {}", internalFileno, refCounter.get());

            other.close();
            
            other.channel = channel;
            other.originalModes = originalModes;
            other.fileDescriptor = fileDescriptor;
            other.refCounter = refCounter;
            other.canBeSeekable = canBeSeekable;
            other.readableChannel = readableChannel;
            other.writableChannel = writableChannel;
            other.seekableChannel = seekableChannel;
        }
    }

    public ChannelDescriptor reopen(Channel channel, ModeFlags modes) {
        return new ChannelDescriptor(channel, internalFileno, modes, fileDescriptor);
    }

    public ChannelDescriptor reopen(RandomAccessFile file, ModeFlags modes) throws IOException {
        return new ChannelDescriptor(file.getChannel(), internalFileno, modes, file.getFD());
    }
    
    /**
     * Perform a low-level seek operation on the associated channel if it is
     * instanceof FileChannel, or raise PipeException if it is not a FileChannel.
     * Calls checkOpen to confirm the target channel is open. This is equivalent
     * to the lseek(2) POSIX function, and like that function it bypasses any
     * buffer flushing or invalidation as in ChannelStream.fseek.
     * 
     * @param offset the offset value to use
     * @param whence whence to seek
     * @throws java.io.IOException If there is an exception while seeking
     * @throws org.jruby.util.io.InvalidValueException If the value specified for
     * offset or whence is invalid
     * @throws org.jruby.util.io.PipeException If the target channel is not seekable
     * @throws org.jruby.util.io.BadDescriptorException If the target channel is
     * already closed.
     * @return the new offset into the FileChannel.
     */
    public long lseek(long offset, int whence) throws IOException, InvalidValueException, PipeException, BadDescriptorException {
        if (seekableChannel) {
            checkOpen();
            
            FileChannel fileChannel = (FileChannel)channel;
            try {
                long pos;
                switch (whence) {
                case PosixShim.SEEK_SET:
                    pos = offset;
                    fileChannel.position(pos);
                    break;
                case PosixShim.SEEK_CUR:
                    pos = fileChannel.position() + offset;
                    fileChannel.position(pos);
                    break;
                case PosixShim.SEEK_END:
                    pos = fileChannel.size() + offset;
                    fileChannel.position(pos);
                    break;
                default:
                    throw new InvalidValueException();
                }
                return pos;
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException();
            } catch (IOException ioe) {
                // "invalid seek" means it's an ESPIPE, so we rethrow as a PipeException()
                if (ioe.getMessage().equals("Illegal seek")) {
                    throw new PipeException();
                }
                throw ioe;
            }
        } else {
            throw new PipeException();
        }
    }

    /**
     * Perform a low-level read of the specified number of bytes into the specified
     * byte list. The incoming bytes will be appended to the byte list. This is
     * equivalent to the read(2) POSIX function, and like that function it
     * ignores read and write buffers defined elsewhere.
     * 
     * @param number the number of bytes to read
     * @param byteList the byte list on which to append the incoming bytes
     * @return the number of bytes actually read
     * @throws java.io.IOException if there is an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed.
     * @see org.jruby.util.ByteList
     */
    public int read(int number, ByteList byteList) throws IOException, BadDescriptorException {
        checkOpen();
        
        byteList.ensure(byteList.length() + number);
        int bytesRead = read(ByteBuffer.wrap(byteList.getUnsafeBytes(),
                byteList.begin() + byteList.length(), number));
        if (bytesRead > 0) {
            byteList.length(byteList.length() + bytesRead);
        }
        return bytesRead;
    }
    
    /**
     * Perform a low-level read of the remaining number of bytes into the specified
     * byte buffer. The incoming bytes will be used to fill the remaining space in
     * the target byte buffer. This is equivalent to the read(2) POSIX function,
     * and like that function it ignores read and write buffers defined elsewhere.
     * 
     * @param buffer the java.nio.ByteBuffer in which to put the incoming bytes
     * @return the number of bytes actually read
     * @throws java.io.IOException if there is an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     * @see java.nio.ByteBuffer
     */
    public int read(ByteBuffer buffer) throws IOException, BadDescriptorException {
        checkOpen();

        // TODO: It would be nice to throw a better error for this
        if (!isReadable()) {
            throw new BadDescriptorException();
        }
        ReadableByteChannel readChannel = (ReadableByteChannel) channel;
        int bytesRead = 0;
        bytesRead = readChannel.read(buffer);

        return bytesRead;
    }
    
    /**
     * Write the bytes in the specified byte list to the associated channel.
     * 
     * @param buffer the byte list containing the bytes to be written
     * @return the number of bytes actually written
     * @throws java.io.IOException if there is an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     */
    public int internalWrite(ByteBuffer buffer) throws IOException, BadDescriptorException {
        checkOpen();

        // TODO: It would be nice to throw a better error for this
        if (!isWritable()) {
            throw new BadDescriptorException();
        }
        
        WritableByteChannel writeChannel = (WritableByteChannel)channel;
        
        // if appendable, we always seek to the end before writing
        if (isSeekable() && originalModes.isAppendable()) {
            // if already in append mode, we don't do our own seeking
            if (!isInAppendMode) {
                FileChannel fileChannel = (FileChannel)channel;
                fileChannel.position(fileChannel.size());
            }
        }
        
        return writeChannel.write(buffer);
    }
    
    /**
     * Write the bytes in the specified byte list to the associated channel.
     * 
     * @param buffer the byte list containing the bytes to be written
     * @return the number of bytes actually written
     * @throws java.io.IOException if there is an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     */
    public int write(ByteBuffer buffer) throws IOException, BadDescriptorException {
        checkOpen();
        
        return internalWrite(buffer);
    }
    
    /**
     * Write the bytes in the specified byte list to the associated channel.
     * 
     * @param buf the byte list containing the bytes to be written
     * @return the number of bytes actually written
     * @throws java.io.IOException if there is an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     */
    public int write(ByteList buf) throws IOException, BadDescriptorException {
        checkOpen();
        
        return internalWrite(ByteBuffer.wrap(buf.getUnsafeBytes(), buf.begin(), buf.length()));
    }

    /**
     * Write the bytes in the specified byte list to the associated channel.
     * 
     * @param buf the byte list containing the bytes to be written
     * @param offset the offset to start at. this is relative to the begin variable in the but
     * @param len the amount of bytes to write. this should not be longer than the buffer
     * @return the number of bytes actually written
     * @throws java.io.IOException if there is an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     */
    public int write(ByteList buf, int offset, int len) throws IOException, BadDescriptorException {
        checkOpen();
        
        return internalWrite(ByteBuffer.wrap(buf.getUnsafeBytes(), buf.begin()+offset, len));
    }
    
    /**
     * Write the byte represented by the specified int to the associated channel.
     * 
     * @param c The byte to write
     * @return 1 if the byte was written, 0 if not and -1 if there was an error
     * (@see java.nio.channels.WritableByteChannel.write(java.nio.ByteBuffer))
     * @throws java.io.IOException If there was an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     */
    public int write(int c) throws IOException, BadDescriptorException {
        checkOpen();
        
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put((byte)c);
        buf.flip();
        
        return internalWrite(buf);
    }

    /**
     * Open a new descriptor using the given working directory, file path,
     * mode flags, and file permission. This is equivalent to the open(2)
     * POSIX function. See org.jruby.util.io.ChannelDescriptor.open(String, String, ModeFlags, int, POSIX)
     * for the version that also sets file permissions.
     *
     * @param cwd the "current working directory" to use when opening the file
     * @param path the file path to open
     * @param flags the mode flags to use for opening the file
     * @return a new ChannelDescriptor based on the specified parameters
     */
    @Deprecated
    public static ChannelDescriptor open(String cwd, String path, ModeFlags flags) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException {
        return open(cwd, path, flags, 0, POSIXFactory.getPOSIX(), null);
    }

    /**
     * Open a new descriptor using the given working directory, file path,
     * mode flags, and file permission. This is equivalent to the open(2)
     * POSIX function. See org.jruby.util.io.ChannelDescriptor.open(String, String, ModeFlags, int, POSIX)
     * for the version that also sets file permissions.
     *
     * @param cwd the "current working directory" to use when opening the file
     * @param path the file path to open
     * @param flags the mode flags to use for opening the file
     * @param classLoader a ClassLoader to use for classpath: resources
     * @return a new ChannelDescriptor based on the specified parameters
     */
    @Deprecated
    public static ChannelDescriptor open(String cwd, String path, ModeFlags flags, ClassLoader classLoader) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException {
        return open(cwd, path, flags, 0, POSIXFactory.getPOSIX(), classLoader);
    }

    /**
     * Open a new descriptor using the given working directory, file path,
     * mode flags, and file permission. This is equivalent to the open(2)
     * POSIX function.
     *
     * @param cwd the "current working directory" to use when opening the file
     * @param path the file path to open
     * @param flags the mode flags to use for opening the file
     * @param perm the file permissions to use when creating a new file (currently
     * unobserved)
     * @param posix a POSIX api implementation, used for setting permissions; if null, permissions are ignored
     * @return a new ChannelDescriptor based on the specified parameters
     */
    @Deprecated
    public static ChannelDescriptor open(String cwd, String path, ModeFlags flags, int perm, POSIX posix) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException {
        return open(cwd, path, flags, perm, posix, null);
    }

    /**
     * Open a new descriptor using the given working directory, file path,
     * mode flags, and file permission. This is equivalent to the open(2)
     * POSIX function.
     *
     * @param cwd the "current working directory" to use when opening the file
     * @param path the file path to open
     * @param flags the mode flags to use for opening the file
     * @param perm the file permissions to use when creating a new file (currently
     * unobserved)
     * @param posix a POSIX api implementation, used for setting permissions; if null, permissions are ignored
     * @param classLoader a ClassLoader to use for classpath: resources
     * @return a new ChannelDescriptor based on the specified parameters
     */
    @Deprecated
    public static ChannelDescriptor open(String cwd, String path, ModeFlags flags, int perm, POSIX posix, ClassLoader classLoader) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException {
        try {
            if (path.equals("/dev/null") || path.equalsIgnoreCase("nul:") || path.equalsIgnoreCase("nul")) {
                Channel nullChannel = new NullChannel();
                // FIXME: don't use RubyIO for this
                return new ChannelDescriptor(nullChannel, flags);
            }

            if (path.startsWith("classpath:/") && classLoader != null) {
                path = path.substring("classpath:/".length());
                InputStream is = classLoader.getResourceAsStream(path);
                // FIXME: don't use RubyIO for this
                return new ChannelDescriptor(Channels.newChannel(is), flags);
            }

            Channel ch = JRubyFile.createResource(Ruby.getGlobalRuntime(), cwd, path).openChannel(flags, perm);
            return new ChannelDescriptor(ch, flags);
        } catch (NullPointerException npe) { npe.printStackTrace(); throw npe; }
    }

    /**
     * Close this descriptor. If in closing the last ChannelDescriptor reference
     * to the associate channel is closed, the channel itself will be closed.
     *
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     * @throws java.io.IOException if there is an exception during IO
     */
    public void close() throws BadDescriptorException, IOException {
        // tidy up
        finish(true);

    }

    void finish(boolean close) throws BadDescriptorException, IOException {
        synchronized (refCounter) {
            // if refcount is at or below zero, we're no longer valid
            if (refCounter.get() <= 0) {
                throw new BadDescriptorException();
            }

            // if channel is already closed, we're no longer valid
            if (!channel.isOpen()) {
                throw new BadDescriptorException();
            }

            // otherwise decrement and possibly close as normal
            if (close) {
                int count = refCounter.decrementAndGet();

                if (DEBUG) LOG.info("Descriptor for fileno {} refs: {}", internalFileno, count);

                if (count <= 0) {
                    // if we're the last referrer, close the channel
                    try {
                        channel.close();
                    } finally {
                        unregisterDescriptor(internalFileno);
                    }
                }
            }
        }
    }

    // Choose something sufficiently high so new IO stuff does not trample as soon
    @Deprecated
    private static final AtomicInteger NEXT_FILENO = new AtomicInteger(Integer.MAX_VALUE / 2);

    @Deprecated
    private static final Map<Integer, ChannelDescriptor> FILENO_MAP = Collections.synchronizedMap(new HashMap<Integer, ChannelDescriptor>());

    @Deprecated
    public static int getNewFileno() {
        return NEXT_FILENO.getAndIncrement();
    }

    @Deprecated
    private static void registerDescriptor(ChannelDescriptor descriptor) {
        FILENO_MAP.put(descriptor.getFileno(), descriptor);
    }

    @Deprecated
    private static void unregisterDescriptor(int aFileno) {
        FILENO_MAP.remove(aFileno);
    }

    @Deprecated
    public static ChannelDescriptor getDescriptorByFileno(int aFileno) {
        return FILENO_MAP.get(aFileno);
    }

    @Deprecated
    public static FileDescriptor getDescriptorFromChannel(Channel channel) {
        return FilenoUtil.getDescriptorFromChannel(channel);
    }

    @Deprecated
    public static int getFilenoFromChannel(Channel channel) {
        return FilenoUtil.filenoFrom(channel);
    }
}
