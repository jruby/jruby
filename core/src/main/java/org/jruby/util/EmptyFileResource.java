package org.jruby.util;

import jnr.constants.platform.Errno;
import jnr.posix.FileStat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.channels.Channel;

class EmptyFileResource implements FileResource {
    // All empty resources are the same and immutable, so may as well
    // cache the instance
    static final EmptyFileResource INSTANCE = new EmptyFileResource();

    public static EmptyFileResource create(String pathname) {
        return (pathname == null || pathname.isEmpty()) ? INSTANCE : null;
    }

    @Override
    public String absolutePath() {
        return "";
    }

    @Override
    public String canonicalPath() {
        return "";
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @Override
    public boolean canRead() {
        return false;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public boolean isSymLink() {
        return false;
    }

    @Override
    public String[] list() { return null; }

    @Override
    public long lastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long length() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileStat stat() {
        return null;
    }

    @Override
    public FileStat lstat() {
        return null;
    }

    @Override
    public int errno() {
        return Errno.ENOENT.intValue();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        throw new ResourceException.NotFound("");
    }

    @Override
    public Channel openChannel(int flags, int perm) throws IOException {
        throw new ResourceException.NotFound(absolutePath());
    }

    @Override
    public <T> T unwrap(Class<T> type) throws UnsupportedOperationException {
        if (type == File.class || type == JRubyFile.class) return (T) JRubyFile.DUMMY;
        throw new UnsupportedOperationException("unwrap: " + type.getName());
    }

    @Override
    public String toString() {
        return "";
    }

}
