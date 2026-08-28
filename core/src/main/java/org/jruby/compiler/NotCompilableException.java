package org.jruby.compiler;



public class NotCompilableException extends RuntimeException {
    private static final long serialVersionUID = 8481162670192366492L;

    public NotCompilableException(String message) {
        super(message);
    }

    public NotCompilableException(Throwable t) {
        super(t);
    }

    public NotCompilableException(String message, Throwable t) {
        super(message, t);
    }
}