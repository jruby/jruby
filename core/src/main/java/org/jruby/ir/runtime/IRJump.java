package org.jruby.ir.runtime;

import org.jruby.util.cli.Options;

/**
 * Created by headius on 3/10/16.
 */
public class IRJump extends RuntimeException {
    public IRJump() {
        super();
    }

    public IRJump(String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
        if (Options.JUMP_BACKTRACE.load()) {
            return super.fillInStackTrace();
        }

        return this;
    }
}
