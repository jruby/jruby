package org.jruby.java.addons;

import org.jruby.RubyArray;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class KernelJavaAddons {

    @JRubyMethod
    public static IRubyObject to_java(ThreadContext context, final IRubyObject fromObject) {
        if ( fromObject instanceof RubyArray ) {
            final JavaClass targetType = context.runtime.getJavaSupport().getObjectJavaClass();
            return targetType.javaArrayFromRubyArray(context, (RubyArray) fromObject);
        }
        return Java.getInstance(context.runtime, fromObject.toJava(Object.class));
    }

    @JRubyMethod
    public static IRubyObject to_java(ThreadContext context, final IRubyObject fromObject, final IRubyObject type) {
        if ( type.isNil() ) return to_java(context, fromObject);

        final JavaClass targetType = resolveTargetType(context, type);
        if ( fromObject instanceof RubyArray ) {
            return targetType.javaArrayFromRubyArray(context, (RubyArray) fromObject);
        }
        return Java.getInstance(context.runtime, fromObject.toJava(targetType.javaClass()));
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_signature(IRubyObject recv, IRubyObject[] args) {
        // empty stub for now
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_name(IRubyObject recv, IRubyObject[] args) {
        // empty stub for now
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_implements(IRubyObject recv, IRubyObject[] args) {
        // empty stub for now
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_annotation(IRubyObject recv, IRubyObject[] args) {
        // empty stub for now
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_require(IRubyObject recv, IRubyObject[] args) {
        // empty stub for now
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_package(IRubyObject recv, IRubyObject[] args) {
        // empty stub for now
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_field(IRubyObject recv, IRubyObject[] args) {
        // empty stub for now
        return recv.getRuntime().getNil();
    }

    static JavaClass resolveTargetType(ThreadContext context, IRubyObject type) {
        JavaClass javaType = JavaClass.resolveType(context, type);
        if ( javaType == null ) throw context.runtime.newTypeError("unable to convert to type: " + type);
        return javaType;
    }

}
