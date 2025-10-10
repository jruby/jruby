package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import org.jruby.util.io.ModeFlags;

import java.nio.channels.Channel;

/**
 * This is a shared interface for files loaded as {@link java.io.File} and {@link java.util.zip.ZipEntry}.
 */
public interface FileResource {

    static FileResource wrap(POSIX posix, JRubyFile file) {
        return new RegularFileResource(posix, file, file.getPathDefault());
    }

    String absolutePath();
    String canonicalPath();
    String path();

    boolean exists();
    boolean isDirectory();
    boolean isFile();
    boolean canExecute();
    int errno();

    long lastModified();
    long length();

    boolean canRead();
    boolean canWrite();

    /**
     * @return is this a NUL device?
     */
    default boolean isNull() { return false; }

    /**
     * @see java.io.File#list()
     */
    String[] list();

    boolean isSymLink();

    FileStat stat();
    FileStat lstat();

    /**
     * Unwrap the resource backend (replacement for {@link #hackyGetJRubyFile()}).
     * @param type
     * @param <T>
     * @return backend if supported
     * @throws UnsupportedOperationException
     */
    <T> T unwrap(Class<T> type) throws UnsupportedOperationException ;


    // For transition to file resources only. Implementations should return
    // JRubyFile if this resource is backed by one, and NOT_FOUND JRubyFile
    // otherwise.
    @Deprecated(since = "9.2.1.0")
    default JRubyFile hackyGetJRubyFile() {
        try {
            return unwrap(JRubyFile.class);
        }
        catch (UnsupportedOperationException ex) {
            return JRubyFile.DUMMY;
        }
    }

    /**
     * @deprecated
     *
     * Opens a new input stream to read the contents of a resource and returns it.
     * Note that implementations may be allocating native memory for the stream, so
     * callers need to close this when they are done with it. users of this 
     * method should follow the pattern: close the stream where you open it.
     *
     * @return just opened InputStream
     * @throws ResourceException is the file does not exists or if the resource is a directory
     */
    @Deprecated(since = "9.4-")
    default InputStream inputStream() throws ResourceException {
        if (!exists()) {
            throw new ResourceException.NotFound(absolutePath());
        }
        if (isDirectory()) {
            throw new ResourceException.FileIsDirectory(absolutePath());
        }
        try {
            return openInputStream();
        }
        catch (IOException e) {
            throw new ResourceException.IOError(e);
        }
    }

    /**Opens a new input stream to read the contents of a resource and returns it.
     *
     * Note that implementations may be allocating native memory for the stream, so
     * callers need to close this when they are done with it. users of this
     * method should follow the pattern: close the stream where you open it.
     *
     * @return InputStream
     * @throws IOException
     */
    InputStream openInputStream() throws IOException ;

    /**
     * @deprecated use {@link #openChannel(int, int)} instead
     *
     * @param flags
     * @param perm
     * @return channel
     * @throws ResourceException
     */
    @Deprecated(since = "9.4-")
    default Channel openChannel(ModeFlags flags, int perm) throws ResourceException {
        try {
            return openChannel(flags.getFlags(), perm);
        }
        catch (ResourceException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new ResourceException.IOError(ex);
        }
    }

    Channel openChannel(int flags, int perm) throws IOException ;

}
