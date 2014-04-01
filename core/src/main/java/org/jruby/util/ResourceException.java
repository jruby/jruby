package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.exceptions.RaisableException;
import org.jruby.exceptions.RaiseException;

class ResourceException extends RaisableException {
    static class FileIsDirectory extends ResourceException {
        public FileIsDirectory(String path) { super("EISDIR", path); }
    }

    static class FileExists extends ResourceException {
        public FileExists(String path) { super("EEXIST", path); }
    }

    static class NotFound extends ResourceException {
        public NotFound(String path) { super("ENOENT", path); }
    }

    static class PermissionDenied extends ResourceException {
        public PermissionDenied(String path) { super("EACCES", path); }
    }

    private final String path;
    private final String errnoClass;

    protected ResourceException(String errnoClass, String path) {
        this.errnoClass = errnoClass;
        this.path = path;
    }

    @Override
    public RaiseException newRaiseException(Ruby runtime) {
        return runtime.newRaiseException(runtime.getErrno().getClass(errnoClass), path);
    }
}
