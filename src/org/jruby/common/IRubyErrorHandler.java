package org.jruby.common;

import org.ablaf.common.IErrorHandler;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IRubyErrorHandler extends IErrorHandler {
    void warn(String message);

    /** 
     * reports only if in verbose mode.
     */
    void warning(String message);
    
    boolean isVerbose();
    void setVerbose(boolean verbose);
}