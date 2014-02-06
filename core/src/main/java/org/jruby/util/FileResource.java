package org.jruby.util;

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
}
