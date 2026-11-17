
package org.jruby.ext.ffi;

import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;

/**
 * Converts data from one FFI type to another.
 */
public class DataConverter {
    public static RubyModule createDataConverterModule(ThreadContext context, RubyModule FFI) {
        return FFI.defineModuleUnder(context, "DataConverter");
    }
}
