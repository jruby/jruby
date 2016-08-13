package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.JavaProxy;
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

        final Ruby runtime = context.runtime;

        final JavaClass targetType = getTargetType(context, runtime, type);
        if ( fromObject instanceof RubyArray ) {
            return targetType.javaArrayFromRubyArray(context, (RubyArray) fromObject);
        }
        return Java.getInstance(runtime, fromObject.toJava(targetType.javaClass()));
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

    private static JavaClass getTargetType(ThreadContext context, Ruby runtime, IRubyObject type) {

        if (type instanceof RubyString || type instanceof RubySymbol) {
            final String className = type.toString();
            JavaClass targetType = runtime.getJavaSupport().getNameClassMap().get(className);
            if ( targetType == null ) targetType = JavaClass.forNameVerbose(runtime, className);
            return targetType;
        }
        if (type instanceof JavaProxy) {
            final Object wrapped = ((JavaProxy) type).getObject();
            if ( wrapped instanceof Class ) {
                return JavaClass.get(runtime, (Class) wrapped);
            }
            throw runtime.newTypeError("not a valid target type: " + type);
        }
        if (type instanceof JavaClass) {
            return (JavaClass) type;
        }
        if (type instanceof RubyModule) {
            IRubyObject java_class = JavaClass.java_class(context, (RubyModule) type);
            if ( ! java_class.isNil() ) return (JavaClass) java_class;
        }

        throw runtime.newTypeError("unable to convert to type: " + type);
    }

}
