package org.jruby.ext.etc;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.runtime.load.Library;

public class EtcLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        var context = runtime.getCurrentContext();
        RubyEtc.createEtcModule(context);
    }
}
