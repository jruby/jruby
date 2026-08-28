package org.jruby.ir.persistence;

public class IRPersistenceException extends Exception {
    public IRPersistenceException(String message) {
        super(message);
    }

    public IRPersistenceException() {
        super();
    }

    public IRPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public IRPersistenceException(Throwable cause) {
        super(cause);
    }
}

