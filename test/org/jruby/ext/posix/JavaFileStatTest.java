/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ext.posix;

import junit.framework.TestCase;

/**
 *
 * @author nicksieger
 */
public class JavaFileStatTest extends TestCase {
    public void testSetup() {
        JavaFileStat fs = new JavaFileStat(null, null);
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            fs.setup("c:/");
        } else {
            fs.setup("/");
        }
        assertFalse(fs.isSymlink());
    }
}
