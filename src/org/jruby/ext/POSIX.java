/*
 * POSIX.java
 * 
 * Created on Sep 1, 2007, 5:50:31 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ext;

import com.sun.jna.Library;

/**
 *
 * @author headius
 */
public interface POSIX extends Library {
    public int chmod(String filename, int mode);
    public int chown(String filename, int user, int group);
    public int getpid();
}
