package org.jruby.ext.posix;

import org.jruby.Ruby;
import org.jruby.ext.posix.POSIX.ERRORS;

public class JRubyPOSIXHandler implements POSIXHandler {
    Ruby runtime;
    
    public JRubyPOSIXHandler(Ruby runtime) {
        this.runtime = runtime;
    }

    public void error(ERRORS error, String extraData) {
        switch (error) {
        case ENOENT:
            throw runtime.newErrnoENOENTError("No such file or directory - " + extraData);
        }
    }

    public void unimplementedError(String method) {
        throw runtime.newNotImplementedError(method + " unsupported on this platform");
    }

    public void warn(String message) {
        runtime.getWarnings().warn(message);
    }
}
