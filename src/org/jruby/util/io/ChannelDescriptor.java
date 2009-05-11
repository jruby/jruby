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
 * Copyright (C) 2008 Charles O Nutter <headius@headius.com>
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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import static java.util.logging.Logger.getLogger;
import java.util.zip.ZipEntry;
import org.jruby.RubyIO;
import org.jruby.ext.posix.POSIX;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyFile;
import static org.jruby.util.io.ModeFlags.*;

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
public class ChannelDescriptor {
    /** Whether to log debugging information */
    private static final boolean DEBUG = false;
    
    /** The java.nio.channels.Channel this descriptor wraps. */
    private Channel channel;
    /**
     * The file number (equivalent to the int file descriptor value in POSIX)
     * for this descriptor.
     */
    private int fileno;
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
     * See {@link #ChannelDescriptor(InputStream, int, ModeFlags, FileDescriptor)}
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
     */
    private ChannelDescriptor(Channel channel, int fileno, ModeFlags originalModes, FileDescriptor fileDescriptor, AtomicInteger refCounter, boolean canBeSeekable) {
        this.refCounter = refCounter;
        this.channel = channel;
        this.fileno = fileno;
        this.originalModes = originalModes;
        this.fileDescriptor = fileDescriptor;
        this.canBeSeekable = canBeSeekable;
    }

    /**
     * Construct a new ChannelDescriptor with the given channel, file number, mode flags,
     * and file descriptor object. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed.
     * 
     * @param channel The channel for the new descriptor
     * @param fileno The file number for the new descriptor
     * @param originalModes The mode flags for the new descriptor
     * @param fileDescriptor The java.io.FileDescriptor object for the new descriptor
     */
    public ChannelDescriptor(Channel channel, int fileno, ModeFlags originalModes, FileDescriptor fileDescriptor) {
        this(channel, fileno, originalModes, fileDescriptor, new AtomicInteger(1), true);
    }

    /**
     * Special constructor to create the ChannelDescriptor out of the stream, file number,
     * mode flags, and file descriptor object. The channel will be created from the
     * provided stream. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed. <b>Note:</b> in most cases, you should not
     * use this constructor, it's reserved mostly for STDIN.
     *
     * @param baseInputStream The stream to create the channel for the new descriptor
     * @param fileno The file number for the new descriptor
     * @param originalModes The mode flags for the new descriptor
     * @param fileDescriptor The java.io.FileDescriptor object for the new descriptor
     */
    public ChannelDescriptor(InputStream baseInputStream, int fileno, ModeFlags originalModes, FileDescriptor fileDescriptor) {
        // The reason why we need the stream is to be able to invoke available() on it.
        // STDIN in Java is non-interruptible, non-selectable, and attempt to read
        // on such stream might lead to thread being blocked without *any* way to unblock it.
        // That's where available() comes it, so at least we could check whether
        // anything is available to be read without blocking.
        this(Channels.newChannel(baseInputStream), fileno, originalModes, fileDescriptor, new AtomicInteger(1), true);
        this.baseInputStream = baseInputStream;
    }

    /**
     * Construct a new ChannelDescriptor with the given channel, file number,
     * and file descriptor object. The channel will be kept open until all ChannelDescriptor
     * references to it have been closed. The channel's capabilities will be used
     * to determine the "original" set of mode flags.
     * 
     * @param channel The channel for the new descriptor
     * @param fileno The file number for the new descriptor
     * @param fileDescriptor The java.io.FileDescriptor object for the new descriptor
     */
    public ChannelDescriptor(Channel channel, int fileno, FileDescriptor fileDescriptor) throws InvalidValueException {
        this(channel, fileno, getModesFromChannel(channel), fileDescriptor);
    }

    /**
     * Get this descriptor's file number.
     * 
     * @return the fileno for this descriptor
     */
    public int getFileno() {
        return fileno;
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
     * See {@link #ChannelDescriptor(InputStream, int, ModeFlags, FileDescriptor)}
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
        return canBeSeekable && channel instanceof FileChannel;
    }
    
    /**
     * Set the channel to be explicitly seekable or not, for streams that appear
     * to be seekable with the instanceof FileChannel check.
     * 
     * @param seekable Whether the channel is seekable or not.
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
        return channel instanceof WritableByteChannel;
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

            int newFileno = RubyIO.getNewFileno();
            
            if (DEBUG) getLogger("ChannelDescriptor").info("Reopen fileno " + newFileno + ", refs now: " + refCounter.get());

            return new ChannelDescriptor(channel, newFileno, originalModes, fileDescriptor, refCounter, canBeSeekable);
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

            if (DEBUG) getLogger("ChannelDescriptor").info("Reopen fileno " + fileno + ", refs now: " + refCounter.get());

            return new ChannelDescriptor(channel, fileno, originalModes, fileDescriptor, refCounter, canBeSeekable);
        }
    }
    
    /**
     * Mimics the POSIX dup2(2) function, returning a new descriptor that references
     * the same open channel but with a specified fileno. This differs from the fileno
     * version by making the target descriptor into a new reference to the current
     * descriptor's channel, closing what it originally pointed to and preserving
     * its original fileno.
     * 
     * @param fileno The fileno to use for the new descriptor
     * @return A duplicate ChannelDescriptor based on this one
     */
    public void dup2Into(ChannelDescriptor other) throws BadDescriptorException, IOException {
        synchronized (refCounter) {
            refCounter.incrementAndGet();

            if (DEBUG) getLogger("ChannelDescriptor").info("Reopen fileno " + fileno + ", refs now: " + refCounter.get());

            other.close();
            
            other.channel = channel;
            other.originalModes = originalModes;
            other.fileDescriptor = fileDescriptor;
            other.refCounter = refCounter;
            other.canBeSeekable = canBeSeekable;
        }
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
        if (channel instanceof FileChannel) {
            checkOpen();
            
            FileChannel fileChannel = (FileChannel)channel;
            try {
                long pos;
                switch (whence) {
                case Stream.SEEK_SET:
                    pos = offset;
                    fileChannel.position(pos);
                    break;
                case Stream.SEEK_CUR:
                    pos = fileChannel.position() + offset;
                    fileChannel.position(pos);
                    break;
                case Stream.SEEK_END:
                    pos = fileChannel.size() + offset;
                    fileChannel.position(pos);
                    break;
                default:
                    throw new InvalidValueException();
                }
                return pos;
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException();
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
     * @see java.util.ByteList
     */
    public int read(int number, ByteList byteList) throws IOException, BadDescriptorException {
        checkOpen();
        
        byteList.ensure(byteList.length() + number);
        int bytesRead = read(ByteBuffer.wrap(byteList.unsafeBytes(), 
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
        if (!(channel instanceof ReadableByteChannel)) {
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
     * @param buf the byte list containing the bytes to be written
     * @return the number of bytes actually written
     * @throws java.io.IOException if there is an exception during IO
     * @throws org.jruby.util.io.BadDescriptorException if the associated
     * channel is already closed
     */
    public int internalWrite(ByteBuffer buffer) throws IOException, BadDescriptorException {
        checkOpen();

        // TODO: It would be nice to throw a better error for this
        if (!(channel instanceof WritableByteChannel)) {
            throw new BadDescriptorException();
        }
        
        WritableByteChannel writeChannel = (WritableByteChannel)channel;
        
        if (isSeekable() && originalModes.isAppendable()) {
            FileChannel fileChannel = (FileChannel)channel;
            fileChannel.position(fileChannel.size());
        }
        
        return writeChannel.write(buffer);
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
        
        return internalWrite(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
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
        
        return internalWrite(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin()+offset, len));
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
     * @throws java.io.FileNotFoundException if the target file could not be found
     * and the create flag was not specified
     * @throws org.jruby.util.io.DirectoryAsFileException if the target file is
     * a directory being opened as a file
     * @throws org.jruby.util.io.FileExistsException if the target file should
     * be created anew, but already exists
     * @throws java.io.IOException if there is an exception during IO
     */
    public static ChannelDescriptor open(String cwd, String path, ModeFlags flags) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException {
        return open(cwd, path, flags, 0, null);
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
     * @throws java.io.FileNotFoundException if the target file could not be found
     * and the create flag was not specified
     * @throws org.jruby.util.io.DirectoryAsFileException if the target file is
     * a directory being opened as a file
     * @throws org.jruby.util.io.FileExistsException if the target file should
     * be created anew, but already exists
     * @throws java.io.IOException if there is an exception during IO
     */
    public static ChannelDescriptor open(String cwd, String path, ModeFlags flags, int perm, POSIX posix) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException {
        boolean fileCreated = false;
        if (path.equals("/dev/null") || path.equalsIgnoreCase("nul:") || path.equalsIgnoreCase("nul")) {
            Channel nullChannel = new NullChannel();
            // FIXME: don't use RubyIO for this
            return new ChannelDescriptor(nullChannel, RubyIO.getNewFileno(), flags, new FileDescriptor());
        } else if (path.startsWith("file:")) {
            int bangIndex = path.indexOf("!");
            if (bangIndex > 0) {
                String filePath = path.substring(5, bangIndex);
                String internalPath = path.substring(bangIndex + 2);

                if (!new File(filePath).exists()) {
                    throw new FileNotFoundException(path);
                }

                JarFile jf = new JarFile(filePath);
                ZipEntry zf = jf.getEntry(internalPath);

                if(zf == null) {
                    throw new FileNotFoundException(path);
                }

                InputStream is = jf.getInputStream(zf);
                // FIXME: don't use RubyIO for this
                return new ChannelDescriptor(Channels.newChannel(is), RubyIO.getNewFileno(), flags, new FileDescriptor());
            } else {
                // raw file URL, just open directly
                URL url = new URL(path);
                InputStream is = url.openStream();
                // FIXME: don't use RubyIO for this
                return new ChannelDescriptor(Channels.newChannel(is), RubyIO.getNewFileno(), flags, new FileDescriptor());
            }
        } else {
            JRubyFile theFile = JRubyFile.create(cwd,path);

            if (theFile.isDirectory() && flags.isWritable()) {
                throw new DirectoryAsFileException();
            }

            if (flags.isCreate()) {
                if (theFile.exists() && flags.isExclusive()) {
                    throw new FileExistsException(path);
                }
                fileCreated = theFile.createNewFile();
            } else {
                if (!theFile.exists()) {
                    throw new FileNotFoundException(path);
                }
            }

            // We always open this rw since we can only open it r or rw.
            RandomAccessFile file = new RandomAccessFile(theFile, flags.toJavaModeString());

            // call chmod after we created the RandomAccesFile
            // because otherwise, the file could be read-only
            if (fileCreated) {
                // attempt to set the permissions, if we have been passed a POSIX instance,
                // and only if the file was created in this call.
                if (posix != null && perm != -1) {
                    posix.chmod(theFile.getPath(), perm);
                }
            }

            if (flags.isTruncate()) file.setLength(0L);

            // TODO: append should set the FD to end, no? But there is no seek(int) in libc!
            //if (modes.isAppendable()) seek(0, Stream.SEEK_END);

            return new ChannelDescriptor(file.getChannel(), RubyIO.getNewFileno(), flags, file.getFD());
        }
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
            int count = refCounter.decrementAndGet();

            if (DEBUG) getLogger("ChannelDescriptor").info("Descriptor for fileno " + fileno + " refs: " + count);

            if (count <= 0) {
                channel.close();
            }
        }
    }
    
    /**
     * Build a set of mode flags using the specified channel's actual capabilities.
     * 
     * @param channel the channel to examine for capabilities
     * @return the mode flags 
     * @throws org.jruby.util.io.InvalidValueException
     */
    private static ModeFlags getModesFromChannel(Channel channel) throws InvalidValueException {
        ModeFlags modes;
        if (channel instanceof ReadableByteChannel) {
            if (channel instanceof WritableByteChannel) {
                modes = new ModeFlags(RDWR);
            } else {
                modes = new ModeFlags(RDONLY);
            }
        } else if (channel instanceof WritableByteChannel) {
            modes = new ModeFlags(WRONLY);
        } else {
            // FIXME: I don't like this
            modes = new ModeFlags(RDWR);
        }
        
        return modes;
    }
}
