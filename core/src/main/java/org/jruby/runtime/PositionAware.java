package org.jruby.runtime;

/**
 * Simple no-pojo methods for Position of a method (in truth we could use
 * this anywhere).  Simpler string and int does not require using
 * ISourcePosition, which is nicer for the
 */
public interface PositionAware {
    /**
     * Get the filename for the method.
     * 
     * @return the filename for the method
     */
    public String getFile();
    
    /**
     * Get the line number for the method. 0-based (ie. line 1 returns a 0)
     * 
     * @return the line number for the method
     */
    public int getLine();
}
