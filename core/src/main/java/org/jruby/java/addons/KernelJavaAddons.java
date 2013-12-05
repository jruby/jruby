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
    @JRubyMethod(name = "raise", optional = 3, module = true, visibility = Visibility.PRIVATE, omit = true)
    public static IRubyObject rbRaise(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        // Check for a Java exception
        ConcreteJavaProxy exception = null;
        if (args.length == 0 && runtime.getGlobalVariables().get("$!") instanceof ConcreteJavaProxy) {
            exception = (ConcreteJavaProxy)runtime.getGlobalVariables().get("$!");
        } else if (args.length == 1 && args[0] instanceof ConcreteJavaProxy) {
            exception = (ConcreteJavaProxy)args[0];
        }

        if (exception != null) {
            // looks like someone's trying to raise a Java exception. Let them.
            Object maybeThrowable = exception.getObject();
            
            if (maybeThrowable instanceof Throwable) {
                // yes, we're cheating here.
                Helpers.throwException((Throwable)maybeThrowable);
                return recv; // not reached
            } else {
                throw runtime.newTypeError("can't raise a non-Throwable Java object");
            }
        } else {
            return RubyKernel.raise(context, recv, args, block);
        }
    }

    @JRubyMethod
    public static IRubyObject to_java(ThreadContext context, IRubyObject fromObject) {
        if (fromObject instanceof RubyArray) {
            return context.runtime.getJavaSupport().getObjectJavaClass().javaArrayFromRubyArray(context, fromObject);
        } else {
            return Java.getInstance(context.runtime, fromObject.toJava(Object.class));
        }
    }
    
    @JRubyMethod
    public static IRubyObject to_java(ThreadContext context, IRubyObject fromObject, IRubyObject type) {
        if (type.isNil()) {
            return to_java(context, fromObject);
        }

        Ruby runtime = context.runtime;
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

    private static JavaClass getTargetType(ThreadContext context, Ruby runtime, IRubyObject type) {
        JavaClass targetType;

        if (type instanceof RubyString || type instanceof RubySymbol) {
            targetType = runtime.getJavaSupport().getNameClassMap().get(type.asJavaString());
            if (targetType == null) targetType = JavaClass.forNameVerbose(runtime, type.asJavaString());
        } else if (type instanceof RubyModule && type.respondsTo("java_class")) {
            targetType = (JavaClass) Helpers.invoke(context, type, "java_class");
        } else if (type instanceof JavaProxy) {
            if  (((JavaProxy)type).getObject() instanceof Class) {
                targetType = JavaClass.get(runtime, (Class)((JavaProxy)type).getObject());
            } else {
                throw runtime.newTypeError("not a valid target type: " + type);
            }
        } else {
            throw runtime.newTypeError("unable to convert to type: " + type);
        }

        return targetType;
    }
}
