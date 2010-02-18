package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.javasupport.*;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.unsafe.UnsafeFactory;

public class KernelJavaAddons {
    @JRubyMethod(name = "raise", optional = 3, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject rbRaise(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        
        if (args.length == 1 && args[0].dataGetStruct() instanceof JavaObject) {
            // looks like someone's trying to raise a Java exception. Let them.
            Object maybeThrowable = ((JavaObject)args[0].dataGetStruct()).getValue();
            
            if (maybeThrowable instanceof Throwable) {
                // yes, we're cheating here.
                UnsafeFactory.getUnsafe().throwException((Throwable)maybeThrowable);
                return recv; // not reached
            } else {
                throw context.getRuntime().newTypeError("can't raise a non-Throwable Java object");
            }
        } else {
            return RubyKernel.raise(context, recv, args, block);
        }
    }

    @JRubyMethod(backtrace = true)
    public static IRubyObject to_java(ThreadContext context, IRubyObject fromObject) {
        if (fromObject instanceof RubyArray) {
            return context.getRuntime().getJavaSupport().getObjectJavaClass().javaArrayFromRubyArray(context, fromObject);
        } else {
            return Java.getInstance(context.getRuntime(), fromObject.toJava(Object.class));
        }
    }
    
    @JRubyMethod(backtrace = true)
    public static IRubyObject to_java(ThreadContext context, IRubyObject fromObject, IRubyObject type) {
        if (type.isNil()) {
            return to_java(context, fromObject);
        }

        Ruby runtime = context.getRuntime();
        JavaClass targetType = getTargetType(context, runtime, type);

        if (fromObject instanceof RubyArray) {
            return targetType.javaArrayFromRubyArray(context, fromObject);
        } else {
            return Java.getInstance(runtime, fromObject.toJava(targetType.javaClass()));
        }
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

    private static JavaClass getTargetType(ThreadContext context, Ruby runtime, IRubyObject type) {
        JavaClass targetType;

        if (type instanceof RubyString || type instanceof RubySymbol) {
            targetType = runtime.getJavaSupport().getNameClassMap().get(type.asJavaString());
            if (targetType == null) targetType = JavaClass.forNameVerbose(runtime, type.asJavaString());
        } else if (type instanceof RubyModule && type.respondsTo("java_class")) {
            targetType = (JavaClass)RuntimeHelpers.invoke(context, type, "java_class");
        } else {
            throw runtime.newTypeError("unable to convert array to type: " + type);
        }

        return targetType;
    }
}
