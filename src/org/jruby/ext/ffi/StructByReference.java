
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;



@JRubyClass(name="FFI::StructByReference", parent="Object")
public final class StructByReference extends RubyObject {
    private final StructLayout structLayout;
    private final RubyClass structClass;

    public static RubyClass createStructByReferenceClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass sbrClass = ffiModule.defineClassUnder("StructByReference", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        sbrClass.defineAnnotatedMethods(StructByReference.class);
        sbrClass.defineAnnotatedConstants(StructByReference.class);
        sbrClass.includeModule(ffiModule.getConstant("DataConverter"));

        return sbrClass;
    }



    @JRubyMethod(name = "new", meta = true)
    public static final IRubyObject newStructByReference(ThreadContext context, IRubyObject klass, IRubyObject structClass) {
        if (!(structClass instanceof RubyClass)) {
            throw context.runtime.newTypeError("wrong argument type "
                    + structClass.getMetaClass().getName() + " (expected Class)");
        }

        if (!((RubyClass) structClass).isKindOfModule(context.runtime.getFFI().structClass)) {
            throw context.runtime.newTypeError("wrong argument type "
                    + structClass.getMetaClass().getName() + " (expected subclass of FFI::Struct)");
        }

        return new StructByReference(context.runtime, (RubyClass) klass,
                (RubyClass) structClass, Struct.getStructLayout(context.runtime, structClass));
    }

    private StructByReference(Ruby runtime, RubyClass klass, RubyClass structClass, StructLayout layout) {
        super(runtime, klass);
        this.structClass = structClass;
        this.structLayout = layout;
    }

    @JRubyMethod(name = "to_s")
    public final IRubyObject to_s(ThreadContext context) {
        return RubyString.newString(context.runtime, String.format("#<FFI::StructByReference:%s>", structClass.getName()));
    }

    @JRubyMethod(name = "layout")
    public final IRubyObject layout(ThreadContext context) {
        return structLayout;
    }

    @JRubyMethod(name = "struct_class")
    public final IRubyObject struct_class(ThreadContext context) {
        return structClass;
    }

    @JRubyMethod(name = "native_type")
    public IRubyObject native_type(ThreadContext context) {
        return context.runtime.getFFI().typeClass.getConstant("POINTER");
    }


    @JRubyMethod(name = "to_native")
    public IRubyObject to_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {
        if (value instanceof Struct && structClass.isInstance(value)) {
            return ((Struct) value).getMemory();

        } else if (value.isNil()) {
            return Pointer.getNull(context.runtime);

        } else {
            throw context.runtime.newTypeError(value, structClass);
        }
    }

    @JRubyMethod(name = "from_native")
    public IRubyObject from_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {
        if (value instanceof AbstractMemory) {
            return getStructClass().newInstance(context, value, Block.NULL_BLOCK);

        } else if (value.isNil()) {
            return getStructClass().newInstance(context, Pointer.getNull(context.runtime), Block.NULL_BLOCK);

        } else {
            throw context.runtime.newTypeError(value, context.runtime.getFFI().pointerClass);
        }
    }

    @JRubyMethod(name = "reference_required?")
    public IRubyObject reference_required_p(ThreadContext context) {
        return context.runtime.getFalse();
    }

    public final StructLayout getStructLayout() {
        return structLayout;
    }

    public final RubyClass getStructClass() {
        return structClass;
    }
}
