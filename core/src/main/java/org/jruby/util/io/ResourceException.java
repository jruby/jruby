package org.jruby.util.io;

import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;
import java.io.IOException;

// While it is public, please don't use this, since in master it will be
// marked private and replaced by RaisableException usage.
public abstract class ResourceException extends IOException {
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

    static class FileIsDirectory extends ErrnoException {
        public FileIsDirectory(String path) { super("EISDIR", path); }
    }

    static class FileExists extends ErrnoException {
        public FileExists(String path) { super("EEXIST", path); }
    }

    static class NotFound extends ErrnoException {
        public NotFound(String path) { super("ENOENT", path); }
    }

    static class PermissionDenied extends ErrnoException {
        public PermissionDenied(String path) { super("EACCES", path); }
    }

    static class IOError extends ResourceException {
        private final IOException ioe;

        IOError(IOException ioe) {
            this.ioe = ioe;
        }

        @Override
        public RaiseException newRaiseException(Ruby runtime) {
            return runtime.newIOErrorFromException(ioe);
        }
    }

    public abstract RaiseException newRaiseException(Ruby runtime);
}
