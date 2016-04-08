package org.jruby.truffle.core.rope;

public class RopeTooLongException extends UnsupportedOperationException {
    public RopeTooLongException(String message) {
        super(message);
    }
}
