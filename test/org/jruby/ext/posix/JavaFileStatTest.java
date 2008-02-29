/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ext.posix;

import junit.framework.TestCase;

import org.jruby.ext.posix.util.Platform;

/**
 *
 * @author nicksieger
 */
public class JavaFileStatTest extends TestCase {
    public void testSetup() {
        JavaFileStat fs = new JavaFileStat(null, null);
        if (Platform.IS_WINDOWS) {
            fs.setup("c:/");
        } else {
            fs.setup("/");
        }
        assertFalse(fs.isSymlink());
    }
}
