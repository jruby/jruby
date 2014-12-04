package org.jruby.util;

import java.io.InputStream;

import jnr.posix.FileStat;

import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

/**
 * This is a shared interface for files loaded as {@link java.io.File} and {@link java.util.zip.ZipEntry}.
 */
public interface FileResource {
    String absolutePath();
    String canonicalPath();

    boolean exists();
    boolean isDirectory();
    boolean isFile();

    long lastModified();
    long length();

    boolean canRead();
    boolean canWrite();

    /**
     * @see java.io.File#list()
     */
    String[] list();

    boolean isSymLink();

    FileStat stat();
    FileStat lstat();

    // For transition to file resources only. Implementations should return
    // JRubyFile if this resource is backed by one, and NOT_FOUND JRubyFile
    // otherwise.
    JRubyFile hackyGetJRubyFile();


    /**
     * opens input-stream to the underlying resource. this is place where
     * the input-stream gets opened. users of this method should follow the pattern:
     * close the stream where you open it.
     *
     * @return just opened InputStream
     * @throws ResourceException is the file does not exists or if the resource is a directory
     */
    InputStream inputStream() throws ResourceException;
    ChannelDescriptor openDescriptor(ModeFlags flags, int perm) throws ResourceException;
}
