package org.jruby.compiler;



public class NotCompilableException extends RuntimeException {
    private static final long serialVersionUID = 8481162670192366492L;

    public NotCompilableException(String message) {
        super(message);
    }
}