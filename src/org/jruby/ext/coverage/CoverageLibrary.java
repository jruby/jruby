package org.jruby.ext.coverage;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.load.Library;

public class CoverageLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) {
        RubyModule coverage = runtime.defineModule("Coverage");
        
        coverage.defineAnnotatedMethods(CoverageModule.class);
    }
}
