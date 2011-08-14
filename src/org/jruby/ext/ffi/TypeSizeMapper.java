package org.jruby.ext.ffi;

import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class TypeSizeMapper {

    private static final int getTypeSize(ThreadContext context, RubyModule ffi, IRubyObject type) {
        if (type instanceof Type) {
            return ((Type) type).getNativeSize();

        } else if (type.isNil()) {
            throw context.getRuntime().newTypeError("nil is invalid FFI type");
        }

        return callTypeSize(context, ffi, type);
    }

    /**
     * Calls up to ruby code to calculate the size of the type
     * 
     * @param context The current thread context
     * @param ffi The FFI ruby module
     * @param sizeArg The name of the type
     * @return size of the type
     */
    private static final int callTypeSize(ThreadContext context, RubyModule ffi, IRubyObject sizeArg) {
        return getTypeSize(context, ffi, ffi.callMethod(context, "find_type", sizeArg));
    }

    public static final int getTypeSize(ThreadContext context, IRubyObject sizeArg) {
        RubyModule ffi = context.getRuntime().getModule("FFI");

        return getTypeSize(context, ffi, ((RubyHash) ffi.fetchConstant("TypeDefs")).fastARef(sizeArg));
    }
}
