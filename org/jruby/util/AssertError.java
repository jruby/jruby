package org.jruby.util;

/** Java 1.3 compatibility class.
 * 
 * @author jpetersen
 */
public class AssertError extends Error {

    /**
     * Constructor for AssertError.
     */
    public AssertError() {
        super();
    }

    /**
     * Constructor for AssertError.
     * @param message
     */
    public AssertError(String message) {
        super(message);
    }
}