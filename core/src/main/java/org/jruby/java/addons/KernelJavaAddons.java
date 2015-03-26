package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.javasupport.*;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
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
        JavaClass targetType;

        if (type instanceof RubyString || type instanceof RubySymbol) {
            targetType = runtime.getJavaSupport().getNameClassMap().get(type.asJavaString());
            if (targetType == null) targetType = JavaClass.forNameVerbose(runtime, type.asJavaString());
        }
        else if (type instanceof RubyModule && type.respondsTo("java_class")) {
            targetType = (JavaClass) Helpers.invoke(context, type, "java_class");
        }
        else if (type instanceof JavaProxy) {
            final Object wrapped = ((JavaProxy) type).getObject();
            if ( wrapped instanceof Class ) {
                targetType = JavaClass.get(runtime, (Class) wrapped);
            } else {
                throw runtime.newTypeError("not a valid target type: " + type);
            }
        } else {
            throw runtime.newTypeError("unable to convert to type: " + type);
        }

        return targetType;
    }
}
