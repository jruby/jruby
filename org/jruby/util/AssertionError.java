package org.jruby.util;

/** Java 1.3 compatibility class.
 * 
 * @author jpetersen
 */
public class AssertionError extends Error {

    /**
     * Constructor for AssertionError.
     */
    public AssertionError() {
        super();
    }

    /**
     * Constructor for AssertionError.
     * @param message
     */
    public AssertionError(String message) {
        super(message);
    }
}