
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;



@JRubyClass(name="FFI::StructByValue", parent="FFI::Type")
public final class StructByValue extends Type {
    private final StructLayout structLayout;
    private final RubyClass structClass;

    public static RubyClass createStructByValueClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass sbvClass = ffiModule.defineClassUnder("StructByValue", ffiModule.fastGetClass("Type"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        sbvClass.defineAnnotatedMethods(StructByValue.class);
        sbvClass.defineAnnotatedConstants(StructByValue.class);

        ffiModule.fastGetClass("Type").fastSetConstant("Struct", sbvClass);

        return sbvClass;
    }

    @JRubyMethod(name = "new", meta = true)
    public static final IRubyObject newStructByValue(ThreadContext context, IRubyObject klass, IRubyObject structClass) {
        if (!(structClass instanceof RubyClass)) {
            throw context.getRuntime().newTypeError("wrong argument type " 
                    + structClass.getMetaClass().getName() + " (expected Class)");
        }
        if (!((RubyClass) structClass).isKindOfModule(context.getRuntime().fastGetModule("FFI").fastGetClass("Struct"))) {
            throw context.getRuntime().newTypeError("wrong argument type " 
                    + structClass.getMetaClass().getName() + " (expected subclass of FFI::Struct)");
        }

        return new StructByValue(context.getRuntime(), (RubyClass) klass, 
                (RubyClass) structClass, Struct.getStructLayout(context.getRuntime(), structClass));
    }

    private StructByValue(Ruby runtime, RubyClass klass, RubyClass structClass, StructLayout structLayout) {
        super(runtime, klass, NativeType.STRUCT);
        this.structClass = structClass;
        this.structLayout = structLayout;
    }

    @JRubyMethod(name = "to_s")
    public final IRubyObject to_s(ThreadContext context) {
        return RubyString.newString(context.getRuntime(), String.format("#<FFI::StructByValue:%s>", structClass.getName()));
    }

    public final StructLayout getStructLayout() {
        return structLayout;
    }

    public final RubyClass getStructClass() {
        return structClass;
    }

    /**
     * Creates a new Ruby FFI::Struct for the class and layout
     *
     * @param runtime The runtime to create the new struct instance in.
     * @param data A byte array containing the data for the struct.
     * @param offset The offset within the array at which the data starts.
     * @return
     */
    public final Struct newStruct(Ruby runtime, byte[] data, int offset) {
        return new Struct(runtime, structClass, structLayout, new Buffer(runtime, data, offset, structLayout.getSize()));
    }
}
