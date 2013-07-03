package org.jruby.ext.nkf;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.runtime.load.Library;

public class NKFLibrary implements Library {

    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyNKF.createNKF(runtime);
    }
}
