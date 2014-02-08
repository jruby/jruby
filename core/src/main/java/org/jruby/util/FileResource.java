package org.jruby.util;

import jnr.posix.FileStat;
import jnr.posix.POSIX;

/**
 * This is a shared interface for files loaded as {@link java.io.File} and {@link java.util.zip.ZipEntry}.
 */
public interface FileResource {
    String absolutePath();

    boolean exists();
    boolean isDirectory();
    boolean isFile();

    long lastModified();
    long length();

    boolean canRead();
    boolean canWrite();

    /**
     * @see java.io.File.list
     */
    String[] list();

    boolean isSymLink();

    FileStat stat(POSIX posix);
    FileStat lstat(POSIX posix);

    // For transition to file resources only. Implementations should return
    // JRubyFile if this resource is backed by one, and NOT_FOUND JRubyFile
    // otherwise.
    JRubyFile hackyGetJRubyFile();
}
