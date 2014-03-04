package org.jruby.util;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;

class EmptyFileResource implements FileResource {
    // All empty resources are the same and immutable, so may as well
    // cache the instance
    private static final EmptyFileResource INSTANCE = new EmptyFileResource();

    public static EmptyFileResource create(String pathname) {
        return (pathname == null || "".equals(pathname)) ?
            INSTANCE : null;
    }

    @Override
    public String absolutePath() {
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
    public String[] list() {
        return new String[0];
    }

    @Override
    public long lastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long length() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileStat stat(POSIX posix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileStat lstat(POSIX posix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JRubyFile hackyGetJRubyFile() {
        // It is somewhat weird that we're returning the NOT_EXIST instance that this resource is
        // intending to replace. However, that should go away once we get rid of the hacky method, so
        // should be okay for now.
        return JRubyNonExistentFile.NOT_EXIST;
    }
}
