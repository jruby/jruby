/*
 * ClosureCallback.java
 *
 * Created on January 4, 2007, 12:32 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

/**
 *
 * @author headius
 */
public interface ClosureCallback {
    public void compile(Compiler context);
}
