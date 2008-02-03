package org.jruby.ext.posix;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * The POSIXHandler class allows you do implement the runtime-specific behavior you need in
 * such a way that it is insulated from the implementation of the POSIX library.  Implementing
 * each of the methods in this interface should give you are working POSIX implementation. 
 *
 */
public interface POSIXHandler {
    public enum WARNING_ID {
        DUMMY_VALUE_USED("DUMMY_VALUE_USED");
        
        private String messageID;

        WARNING_ID(String messageID) {
            this.messageID = messageID;
        }
    }
    public void error(POSIX.ERRORS error, String extraData);
    public void unimplementedError(String message);
    public void warn(WARNING_ID id, String message, Object... data);

    /**
     * Get current working directory of your runtime.
     */
    public File getCurrentWorkingDirectory();
    
    /**
     * Get current set of environment variables of your runtime.
     */
    public String[] getEnv();
    
    /**
     * Get your runtime's current InputStream
     */
    public InputStream getInputStream();
    
    /**
     * Get your runtime's current OutputStream
     */
    public PrintStream getOutputStream();
    
    /**
     * Get your runtimes process ID.  This is only intended for non-native POSIX support (e.g.
     * environments where JNA cannot load or security restricted environments).  In JRuby we
     * found a number of packages which would rather have some identity for the runtime than
     * nothing.
     * 
     * Note: If you do not want this to work you impl can just call unimplementedError(String).
     */
    public int getPID();
    
    /**
     * Get your runtime's current ErrorStream
     */
    public PrintStream getErrorStream();
}
