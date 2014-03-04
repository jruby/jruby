package org.jruby.util.io;

import org.jruby.Ruby;
import org.jruby.exceptions.RaisableException;
import org.jruby.exceptions.RaiseException;

class ErrnoException extends RaisableException {
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
