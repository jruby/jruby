package org.jruby.ext.jruby;

import org.jruby.Ruby;
import org.jruby.runtime.load.Library;
import java.io.IOException;

public class JRubySerializationLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        JRubyObjectInputStream.createJRubyObjectInputStream(runtime);
    }
}