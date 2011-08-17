package org.jruby.ext.coverage;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Implementation of Ruby 1.9.2's "Coverage" module
 */
public class CoverageModule {
    @JRubyMethod(module = true)
    public static IRubyObject start(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        
        if (!runtime.getCoverageData().isCoverageEnabled()) {
            runtime.getCoverageData().setCoverageEnabled(runtime, true);
        }
        
        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject result(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        
        if (!runtime.getCoverageData().isCoverageEnabled()) {
            throw runtime.newRuntimeError("coverage measurement is not enabled");
        }
        
        Map<String, Integer[]> coverage = runtime.getCoverageData().resetCoverage(runtime);
        
        // populate a Ruby Hash with coverage data
        RubyHash covHash = RubyHash.newHash(runtime);
        for (Map.Entry<String, Integer[]> entry : coverage.entrySet()) {
            RubyArray ary = RubyArray.newArray(runtime, entry.getValue().length);
            for (int i = 0; i < entry.getValue().length; i++) {
                Integer integer = entry.getValue()[i];
                ary.store(i, integer == null ? runtime.getNil() : runtime.newFixnum(integer));
                covHash.fastASetCheckString(runtime, RubyString.newString(runtime, entry.getKey()), ary);
            }
        }
        
        return covHash;
    }
    
}
