package org.jruby.libraries;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyEtc;
import org.jruby.runtime.load.Library;

public class EtcLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyEtc.createEtcModule(runtime);
    }
}
