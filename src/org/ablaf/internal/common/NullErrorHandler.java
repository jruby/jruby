package org.ablaf.internal.common;

import org.ablaf.common.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class NullErrorHandler implements IErrorHandler {

    /**
     * Constructor for NullErrorHandler.
     */
    public NullErrorHandler() {
        super();
    }

    /**
     * @see IErrorHandler#isHandled(int)
     */
    public boolean isHandled(int type) {
        return false;
    }

    /**
     * @see IErrorHandler#handleError(int, String)
     */
    public void handleError(int type, String message) {
    }

    /**
     * @see IErrorHandler#handleError(int, ISourcePosition, String)
     */
    public void handleError(int type, ISourcePosition position, String message) {
    }

    /**
     * @see IErrorHandler#handleError(int, ISourcePosition, String, Object)
     */
    public void handleError(int type, ISourcePosition position, String message, Object args) {
    }
}