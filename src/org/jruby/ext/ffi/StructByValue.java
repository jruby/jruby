
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
        RubyClass sbvClass = ffiModule.defineClassUnder("StructByValue", ffiModule.getClass("Type"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        sbvClass.defineAnnotatedMethods(StructByValue.class);
        sbvClass.defineAnnotatedConstants(StructByValue.class);

        ffiModule.getClass("Type").setConstant("Struct", sbvClass);

        return sbvClass;
    }

    @JRubyMethod(name = "new", meta = true)
    public static final IRubyObject newStructByValue(ThreadContext context, IRubyObject klass, IRubyObject structClass) {
        if (!(structClass instanceof RubyClass)) {
            throw context.runtime.newTypeError("wrong argument type "
                    + structClass.getMetaClass().getName() + " (expected Class)");
        }
        if (!((RubyClass) structClass).isKindOfModule(context.runtime.getFFI().structClass)) {
            throw context.runtime.newTypeError("wrong argument type "
                    + structClass.getMetaClass().getName() + " (expected subclass of FFI::Struct)");
        }

        return new StructByValue(context.runtime, (RubyClass) klass,
                (RubyClass) structClass, Struct.getStructLayout(context.runtime, structClass));
    }

    private StructByValue(Ruby runtime, RubyClass klass, RubyClass structClass, StructLayout structLayout) {
        super(runtime, klass, NativeType.STRUCT, structLayout.size, structLayout.alignment);
        this.structClass = structClass;
        this.structLayout = structLayout;
    }

    StructByValue(Ruby runtime, RubyClass structClass, StructLayout structLayout) {
        super(runtime, runtime.getModule("FFI").getClass("Type").getClass("Struct"),
                NativeType.STRUCT, structLayout.size, structLayout.alignment);
        this.structClass = structClass;
        this.structLayout = structLayout;
    }

    @JRubyMethod(name = "to_s")
    public final IRubyObject to_s(ThreadContext context) {
        return RubyString.newString(context.runtime, String.format("#<FFI::StructByValue:%s>", structClass.getName()));
    }

    @JRubyMethod(name = "layout")
    public final IRubyObject layout(ThreadContext context) {
        return structLayout;
    }

    @JRubyMethod(name = "struct_class")
    public final IRubyObject struct_class(ThreadContext context) {
        return structClass;
    }

    public final StructLayout getStructLayout() {
        return structLayout;
    }

    public final RubyClass getStructClass() {
        return structClass;
    }
}
