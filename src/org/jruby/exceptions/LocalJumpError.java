package org.jruby.exceptions;

import org.jruby.Ruby;

/**
 * @author jpeterse
 */
public class LocalJumpError extends RaiseException {
    public LocalJumpError(Ruby runtime, String msg) {
        super(runtime, runtime.getExceptions().getLocalJumpError(), msg, true);
    }
}
