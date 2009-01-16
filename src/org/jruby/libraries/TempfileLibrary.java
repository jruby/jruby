/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.libraries;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyTempfile;
import org.jruby.runtime.load.Library;

/**
 *
 * @author enebo
 */
public class TempfileLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyTempfile.createTempfileClass(runtime);
    }
}
