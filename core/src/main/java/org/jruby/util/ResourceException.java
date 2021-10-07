package org.jruby.util;

import jnr.constants.platform.Errno;
import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;

import java.io.IOException;

/**
 * @note Private API that might get removed later.
 */
// While it is public, please don't use this, since in master it will be
// marked private and replaced by RaiseException usage.
public abstract class ResourceException extends IOException {
    public ResourceException() {}
    public ResourceException(String message) {
        super(message);
    }
    public ResourceException(Throwable t) {
        super(t);
    }
    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class ErrnoException extends ResourceException {
        private final String path;
        private final String errnoClass;
        private final Errno errno;

        protected ErrnoException(String errnoClass, String path) {
            super(errnoClass + ": " + path);
            this.errno = Errno.valueOf(errnoClass);
            this.errnoClass = errnoClass;
            this.path = path;
        }

        protected ErrnoException(Errno errno, String path) {
            super(errno.name() + ": " + path);
            this.errno = errno;
            this.errnoClass = errno.name();
            this.path = path;
        }

        @Override
        public Throwable fillInStackTrace() { return this; }

        @Override
        public RaiseException newRaiseException(Ruby runtime) {
            return runtime.newRaiseException(runtime.getErrno().getClass(errnoClass), path);
        }

        public String getPath() {
            return path;
        }

        public Errno getErrno() {
            return errno;
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

    @Deprecated
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
