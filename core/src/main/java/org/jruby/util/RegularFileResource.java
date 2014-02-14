package org.jruby.util;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyFile;

import jnr.posix.JavaSecuredFile;
import org.jruby.platform.Platform;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.io.IOException;

/**
 * Represents a "regular" file, backed by regular file system.
 */
class RegularFileResource implements FileResource {
    private final JRubyFile file;
    private final POSIX symlinkPosix = POSIXFactory.getPOSIX();

    RegularFileResource(File file) {
        this(file.getAbsolutePath());
    }

    protected RegularFileResource(String filename) {
        this.file = new JRubyFile(filename);
    }

    @Override
    public String absolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public boolean exists() {
        // MRI behavior: Even broken symlinks should return true.
        return file.exists() || isSymLink();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isSymLink() {
        try {
            return symlinkPosix.lstat(absolutePath()).isSymlink();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean canRead() {
        return file.canRead();
    }

    @Override
    public boolean canWrite() {
        return file.canWrite();
    }

    @Override
    public String[] list() {
        return file.list();
    }

    @Override
    public FileStat stat(POSIX posix) {
        return posix.stat(absolutePath());
    }

    @Override
    public FileStat lstat(POSIX posix) {
        return posix.lstat(absolutePath());
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public JRubyFile hackyGetJRubyFile() {
      return file;
    }
}
