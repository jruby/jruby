
package org.jruby.ext.ffi;

import org.jruby.Ruby;
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
        result.defineAnnotatedMethods(DataConverter.class);
        result.defineAnnotatedConstants(DataConverter.class);

        return result;
    }

    private static RubyModule module(IRubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw obj.getRuntime().newTypeError("not a module");
        }

        return (RubyModule) obj;
    }

    @JRubyMethod(name = "native_type", module=true, optional = 1)
    public static IRubyObject native_type(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        RubyModule m = module(self);

        if (args.length == 0) {
            if (!m.hasInternalVariable("native_type")) {
                throw context.runtime.newNotImplementedError("native_type method not overridden and no native_type set");
            }

            return (Type) m.getInternalVariable("native_type");

        } else if (args.length == 1) {
            Type type = Util.findType(context, args[0]);

            m.setInternalVariable("native_type", type);

            return type;

        } else {
            throw context.runtime.newArgumentError("incorrect arguments");
        }
    }


    @JRubyMethod(name = "to_native", module=true)
    public static IRubyObject to_native(ThreadContext context, IRubyObject self, IRubyObject value, IRubyObject ctx) {
        return value;
    }

    @JRubyMethod(name = "from_native", module=true)
    public static IRubyObject from_native(ThreadContext context, IRubyObject self, IRubyObject value, IRubyObject ctx) {
        return value;
    }

    @JRubyMethod(name = "reference_required?", module=true)
    public static IRubyObject reference_required_p(ThreadContext context, IRubyObject self) {
        Object ref = module(self).getInternalVariable("reference_required");
        return context.runtime.newBoolean(!(ref instanceof IRubyObject) || ((IRubyObject) ref).isTrue());
    }

    @JRubyMethod(name = "reference_required", module=true, optional = 1)
    public static IRubyObject reference_required(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        module(self).setInternalVariable("reference_required", context.runtime.newBoolean(args.length < 1 || args[0].isTrue()));
        return self;
    }
}
