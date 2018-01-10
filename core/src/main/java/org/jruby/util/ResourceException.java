package org.jruby.util;

import jnr.constants.platform.Errno;
import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;
import java.io.IOException;

// While it is public, please don't use this, since in master it will be
// marked private and replaced by RaisableException usage.
public abstract class ResourceException extends IOException {
    public ResourceException() {}
    public ResourceException(Throwable t) {
        super(t);
    }

    abstract static class ErrnoException extends ResourceException {
        private final String path;
        private final String errnoClass;

        protected ErrnoException(String errnoClass, String path) {
            this.errnoClass = errnoClass;
            this.path = path;
        }

        @Override
        public RaiseException newRaiseException(Ruby runtime) {
            return runtime.newRaiseException(runtime.getErrno().getClass(errnoClass), path);
        }
    }

    public static class FileIsDirectory extends ErrnoException {
        public FileIsDirectory(String path) { super("EISDIR", path); }
    }

    public static class FileIsNotDirectory extends ErrnoException {
        public FileIsNotDirectory(String path) { super ("ENOTDIR", path); }
    }

    public static class FileExists extends ErrnoException {
        public FileExists(String path) { super("EEXIST", path); }
    }

    public static class NotFound extends ErrnoException {
        public NotFound(String path) { super("ENOENT", path); }
    }

    public static class PermissionDenied extends ErrnoException {
        public PermissionDenied(String path) { super("EACCES", path); }
    }

    public static class InvalidArguments extends ErrnoException {
        public InvalidArguments(String path) { super("EINVAL", path); }
    }

    public static class TooManySymlinks extends ErrnoException {
        public TooManySymlinks(String path) { super("ELOOP", path); }
    }

    public static class IOError extends ResourceException {
        private final IOException ioe;

        IOError(IOException ioe) {
            super(ioe);
            this.ioe = ioe;
        }

        @Override
        public RaiseException newRaiseException(Ruby runtime) {
            return runtime.newIOErrorFromException(ioe);
        }
    }

    public abstract RaiseException newRaiseException(Ruby runtime);
}
