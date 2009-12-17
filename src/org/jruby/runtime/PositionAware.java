package org.jruby.runtime;

/**
 * Simple no-pojo methods for Position of a method (in truth we could use
 * this anywhere).  Simpler string and int does not require using
 * ISourcePosition, which is nicer for the
 */
public interface PositionAware {
    public String getFile();
    public int getLine();
}
