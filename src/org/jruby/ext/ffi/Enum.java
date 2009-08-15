
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a C enum
 */
@JRubyClass(name="FFI::Enum", parent="FFI::Type")
public class Enum extends Type {

    public static RubyClass createEnumClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass enumClass = ffiModule.defineClassUnder("Enum", ffiModule.fastGetClass("Type"),
                Allocator.INSTANCE);
        enumClass.defineAnnotatedMethods(Enum.class);
        enumClass.defineAnnotatedConstants(Enum.class);

        
        return enumClass;
    }

    private static final class Allocator implements ObjectAllocator {
        private static final ObjectAllocator INSTANCE = new Allocator();

        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Enum(runtime, klass);
        }
    }

    private Enum(Ruby runtime, RubyClass klass) {
        super(runtime, klass, NativeType.INT);
    }
}
