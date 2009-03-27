
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.ObjectAllocator;

/**
 *
 */
@JRubyClass(name = "FFI::Type", parent = "Object")
public class Type extends RubyObject {

    public static RubyClass createTypeClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass typeClass = ffiModule.defineClassUnder("Type", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        typeClass.defineAnnotatedMethods(Type.class);
        typeClass.defineAnnotatedConstants(Type.class);

        RubyClass builtinClass = typeClass.defineClassUnder("Builtin", typeClass,
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        RubyModule nativeType = ffiModule.defineModuleUnder("NativeType");
        for (NativeType t : NativeType.values()) {
            Builtin  b = new Builtin(runtime, builtinClass, t);
            typeClass.fastSetConstant(t.name(), b);
            nativeType.fastSetConstant(t.name(), b);
            ffiModule.fastSetConstant("TYPE_" + t.name(), b);
        }

        return typeClass;
    }

    /**
     * Initializes a new <tt>Type</tt> instance.
     */
    protected Type(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @JRubyClass(name = "FFI::Type::Builtin", parent = "FFI::Type")
    final static class Builtin extends Type /* implements NativeParam */ {
        final NativeType nativeType;

        /**
         * Initializes a new <tt>BuiltinType</tt> instance.
         */
        private Builtin(Ruby runtime, RubyClass klass, NativeType nativeType) {
            super(runtime, klass);
            this.nativeType = nativeType;
        }
    }
}
