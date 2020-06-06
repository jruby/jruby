
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Converts data from one FFI type to another.
 */
public class DataConverter {
    public static RubyModule createDataConverterModule(Ruby runtime, RubyModule module) {
        RubyModule result = module.defineModuleUnder("DataConverter");

        return result;
    }
}
