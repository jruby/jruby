package org.jruby.util.collections;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class StackEmptyException extends RuntimeException {

    /**
     * Constructor for StackEmptyException.
     * @param msg
     */
    public StackEmptyException(String msg) {
        super(msg);
    }
}
