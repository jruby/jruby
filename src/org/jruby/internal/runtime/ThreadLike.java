/*
 * ThreadLike.java
 *
 * Created on March 20, 2007, 2:00 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.internal.runtime;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;

/**
 *
 * @author headius
 */
public interface ThreadLike {
    public void start();
    
    public void interrupt();
    
    public boolean isAlive();
    
    public void join() throws InterruptedException, ExecutionException;
    
    public void join(long millis) throws InterruptedException, ExecutionException, TimeoutException;
    
    public int getPriority();
    
    public void setPriority(int priority);
    
    public boolean isCurrent();
    
    public boolean isInterrupted();
}
