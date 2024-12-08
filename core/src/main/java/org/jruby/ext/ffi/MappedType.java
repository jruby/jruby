/*
 * 
 */

package org.jruby.ext.ffi;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;

import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

/**
 * A type which represents a conversion to/from a native type.
 */
@JRubyClass(name="FFI::Type::Mapped", parent="FFI::Type")
public final class MappedType extends Type {
    private final Type realType;
    private final IRubyObject converter;
    private final boolean isReferenceRequired;
    private final CachingCallSite toNativeCallSite = new FunctionalCachingCallSite("to_native");
    private final CachingCallSite fromNativeCallSite = new FunctionalCachingCallSite("from_native");

    public static RubyClass createConverterTypeClass(ThreadContext context, RubyClass Type) {
        return Type.defineClassUnder(context, "Mapped", Type, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, MappedType.class).defineConstants(context, MappedType.class);
    }

    private MappedType(Ruby runtime, RubyClass klass, Type nativeType, IRubyObject converter, boolean isRefererenceRequired) {
        super(runtime, klass, NativeType.MAPPED, nativeType.getNativeSize(), nativeType.getNativeAlignment());
        this.realType = nativeType;
        this.converter = converter;
        this.isReferenceRequired = isRefererenceRequired;
    }

    @JRubyMethod(name = "new", meta = true)
    public static final IRubyObject newMappedType(ThreadContext context, IRubyObject klass, IRubyObject converter) {
        if (!converter.respondsTo("native_type")) {
            throw context.runtime.newNoMethodError("converter needs a native_type method", "native_type", converter.getMetaClass());
        }

        Type nativeType;
        try {
            nativeType = (Type) converter.callMethod(context, "native_type");
        } catch (ClassCastException ex) {
            throw typeError(context, "native_type did not return instance of FFI::Type");
        }

        boolean isReferenceRequired;
        if (converter.respondsTo("reference_required?")) {
            isReferenceRequired = converter.callMethod(context, "reference_required?").isTrue();

        } else {
            switch (nativeType.nativeType) {
                case BOOL:
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG:
                case ULONG:
                case LONG_LONG:
                case ULONG_LONG:
//                case LONGDOUBLE:
                case FLOAT:
                case DOUBLE:
                    isReferenceRequired = false;
                    break;

                default:
                    isReferenceRequired = true;
                    break;
            }
        }

        return new MappedType(context.runtime, (RubyClass) klass, nativeType, converter, isReferenceRequired);
    }
    
    public final Type getRealType() {
        return realType;
    }

    public final boolean isReferenceRequired() {
        return isReferenceRequired;
    }

    public final boolean isPostInvokeRequired() {
        return false;
    }

    @JRubyMethod
    public final IRubyObject native_type(ThreadContext context) {
        return realType;
    }

    @JRubyMethod
    public final IRubyObject from_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {
        return fromNative(context, value);
    }

    @JRubyMethod
    public final IRubyObject to_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {
        return toNative(context, value);
    }

    public final IRubyObject fromNative(ThreadContext context, IRubyObject value) {
        return fromNativeCallSite.call(context, this, converter, value, context.nil);
    }

    public final IRubyObject toNative(ThreadContext context, IRubyObject value) {
        return toNativeCallSite.call(context, this, converter, value, context.nil);
    }
}
