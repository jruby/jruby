package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.util.ArrayUtils;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Array;

public class KernelJavaAddons {

    @JRubyMethod
    public static IRubyObject to_java(ThreadContext context, final IRubyObject fromObject) {
        return fromObject instanceof RubyArray ary ?
                toJavaArray(context, Object.class, ary) :
                Java.getInstance(context.runtime, fromObject.toJava(Object.class));
    }

    @JRubyMethod
    public static IRubyObject to_java(ThreadContext context, final IRubyObject fromObject, final IRubyObject type) {
        if ( type.isNil() ) return to_java(context, fromObject);

        final Class targetType = Java.resolveClassType(context, type);
        if ( fromObject instanceof RubyArray ) {
            return toJavaArray(context, targetType, (RubyArray) fromObject);
        }
        return Java.getInstance(context.runtime, fromObject.toJava(targetType));
    }

    static ArrayJavaProxy toJavaArray(ThreadContext context, final Class<?> type, final RubyArray fromArray) {
        final Object newArray = toJavaArrayInternal(context, type, fromArray);
        return new ArrayJavaProxy(context.runtime, Java.getProxyClassForObject(context, newArray), newArray, JavaUtil.getJavaConverter(type));
    }

    private static Object toJavaArrayInternal(ThreadContext context, final Class<?> type, final RubyArray fromArray) {
        final Object newArray = Array.newInstance(type, fromArray.size());

        if (type.isArray()) {
            // if it's an array of arrays, recurse with the component type
            for ( int i = 0; i < fromArray.size(); i++ ) {
                final Class<?> nestedType = type.getComponentType();
                final IRubyObject element = fromArray.eltInternal(i);
                final Object nestedArray;
                if ( element instanceof RubyArray ) { // recurse
                    nestedArray = toJavaArrayInternal(context, nestedType, (RubyArray) element);
                }
                else if ( type.isInstance(element) ) {
                    nestedArray = element;
                }
                else { // still try (nested) toJava conversion :
                    nestedArray = element.toJava(type);
                }
                ArrayUtils.setWithExceptionHandlingDirect(context.runtime, newArray, i, nestedArray);
            }
        } else {
            ArrayUtils.copyDataToJavaArrayDirect(fromArray, newArray);
        }

        return newArray;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject java_signature(IRubyObject recv, IRubyObject[] args) {
        return java_signature(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_signature(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.nil;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject java_name(IRubyObject recv, IRubyObject[] args) {
        return java_name(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_name(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.nil;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject java_implements(IRubyObject recv, IRubyObject[] args) {
        return java_implements(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_implements(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.nil;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject java_annotation(IRubyObject recv, IRubyObject[] args) {
        return java_annotation(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_annotation(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.nil;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject java_require(IRubyObject recv, IRubyObject[] args) {
        return java_require(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_require(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.nil;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject java_package(IRubyObject recv, IRubyObject[] args) {
        return java_package(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_package(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.nil;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject java_field(IRubyObject recv, IRubyObject[] args) {
        return java_field(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject java_field(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.nil;
    }

}
