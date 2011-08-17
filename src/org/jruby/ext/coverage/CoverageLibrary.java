package org.jruby.ext.coverage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

public class CoverageLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) {
        RubyModule coverage = runtime.defineModule("Coverage");
        
        coverage.defineAnnotatedMethods(CoverageModule.class);
    }
}
