package org.jruby.util;

import jnr.constants.platform.Errno;
import jnr.posix.FileStat;
import org.jruby.util.io.ModeFlags;
import java.io.InputStream;

import java.nio.channels.Channel;

public class EmptyFileResource implements FileResource {
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
    public String canonicalPath() {
        return "";
    }

    @Override
    public boolean exists() {
        return false;
    }

    public int errno() {
        return Errno.ENOENT.intValue();
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
    public String[] list() {
        return StringSupport.EMPTY_STRING_ARRAY;
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
    public FileStat stat() {
        return null;
    }

    @Override
    public FileStat lstat() {
        return null;
    }

    @Override
    public JRubyFile hackyGetJRubyFile() {
        // It is somewhat weird that we're returning the NOT_EXIST instance that this resource is
        // intending to replace. However, that should go away once we get rid of the hacky method, so
        // should be okay for now.
        return JRubyFile.DUMMY;
    }

    @Override
    public InputStream inputStream() throws ResourceException {
        throw new ResourceException.NotFound("");
    }

    @Override
    public Channel openChannel(ModeFlags flags, int perm) throws ResourceException {
        throw new ResourceException.NotFound(absolutePath());
    }
}
