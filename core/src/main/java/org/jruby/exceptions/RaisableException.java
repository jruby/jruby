package org.jruby.exceptions;

import org.jruby.Ruby;

public abstract class RaisableException extends Exception {
    abstract public RaiseException newRaiseException(Ruby ruby);
}
