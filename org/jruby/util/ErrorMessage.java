package org.jruby.util;

/** ErrorMessage represents the generation of an 
 * error message.
 * 
 * @author jpetersen
 */
public interface ErrorMessage {
    /** This method is called if an error occured
     * or an assert failed, you should overwrite the
     * method and append your error message to the buffer.
     */
    public void generate(StringBuffer buffer);
}