package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
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
        final Ruby runtime = context.runtime;
        if ( fromObject instanceof RubyArray ) {
            return toJavaArray(runtime, Object.class, (RubyArray) fromObject);
        }
        return Java.getInstance(runtime, fromObject.toJava(Object.class));
    }

    @JRubyMethod
    public static IRubyObject to_java(ThreadContext context, final IRubyObject fromObject, final IRubyObject type) {
        if ( type.isNil() ) return to_java(context, fromObject);

        final Class targetType = Java.resolveClassType(context, type);
        if ( fromObject instanceof RubyArray ) {
            return toJavaArray(context.runtime, targetType, (RubyArray) fromObject);
        }
        return Java.getInstance(context.runtime, fromObject.toJava(targetType));
    }

    static ArrayJavaProxy toJavaArray(final Ruby runtime, final Class<?> type, final RubyArray fromArray) {
        final Object newArray = toJavaArrayInternal(runtime, type, fromArray);
        return new ArrayJavaProxy(runtime, Java.getProxyClassForObject(runtime, newArray), newArray, JavaUtil.getJavaConverter(type));
    }

    private static Object toJavaArrayInternal(final Ruby runtime, final Class<?> type, final RubyArray fromArray) {
        final Object newArray = Array.newInstance(type, fromArray.size());

        if (type.isArray()) {
            // if it's an array of arrays, recurse with the component type
            for ( int i = 0; i < fromArray.size(); i++ ) {
                final Class<?> nestedType = type.getComponentType();
                final IRubyObject element = fromArray.eltInternal(i);
                final Object nestedArray;
                if ( element instanceof RubyArray ) { // recurse
                    nestedArray = toJavaArrayInternal(runtime, nestedType, (RubyArray) element);
                }
                else if ( type.isInstance(element) ) {
                    nestedArray = element;
                }
                else { // still try (nested) toJava conversion :
                    nestedArray = element.toJava(type);
                }
                ArrayUtils.setWithExceptionHandlingDirect(runtime, newArray, i, nestedArray);
            }
        } else {
            ArrayUtils.copyDataToJavaArrayDirect(fromArray, newArray);
        }

        return newArray;
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

}
