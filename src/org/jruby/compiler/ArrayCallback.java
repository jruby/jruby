/*
 * ArrayCallback.java
 *
 * Created on January 3, 2007, 3:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

/**
 *
 * @author headius
 */
public interface ArrayCallback {
    public void nextValue(Compiler context, Object sourceArray, int index);
}
