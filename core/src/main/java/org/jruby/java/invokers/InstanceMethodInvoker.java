package org.jruby.java.invokers;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;

public final class InstanceMethodInvoker extends MethodInvoker {
    public InstanceMethodInvoker(RubyModule host, Supplier<Method[]> methods, String name) {
        super(host, methods, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        Object target = unwrapIfJavaProxy(self);
        JavaMethod method = (JavaMethod) findCallable(self, name, args, args.length);
        return method.invokeDirect( context, target, convertArguments(method, args) );
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY);
        Object target = unwrapIfJavaProxy(self);
        JavaMethod method = (JavaMethod) findCallableArityZero(self, name);
        return method.invokeDirect(context, target);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0});
        Object target = unwrapIfJavaProxy(self);
        JavaMethod method = (JavaMethod) findCallableArityOne(self, name, arg0);
        final Class<?>[] paramTypes = method.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);
        return method.invokeDirect(context, target, cArg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1});
        Object target = unwrapIfJavaProxy(self);
        JavaMethod method = (JavaMethod) findCallableArityTwo(self, name, arg0, arg1);
        final Class<?>[] paramTypes = method.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);
        Object cArg1 = arg1.toJava(paramTypes[1]);
        return method.invokeDirect(context, target, cArg0, cArg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2});
        Object target = unwrapIfJavaProxy(self);
        JavaMethod method = (JavaMethod) findCallableArityThree(self, name, arg0, arg1, arg2);
        final Class<?>[] paramTypes = method.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);
        Object cArg1 = arg1.toJava(paramTypes[1]);
        Object cArg2 = arg2.toJava(paramTypes[2]);
        return method.invokeDirect(context, target, cArg0, cArg1, cArg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if ( block.isGiven() ) {
            Object target = unwrapIfJavaProxy(self);
            final int len = args.length;
            // these extra arrays are really unfortunate; split some of these paths out to eliminate?
            IRubyObject[] newArgs = ArraySupport.newCopy(args, RubyProc.newProc(context.runtime, block, block.type));

            JavaMethod method = (JavaMethod) findCallable(self, name, newArgs, len + 1);
            final Class<?>[] paramTypes = method.getParameterTypes();

            Object[] convertedArgs = new Object[len + 1];
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = newArgs[i].toJava(paramTypes[i]);
            }

            return method.invokeDirect(context, target, convertedArgs);
        }
        return call(context, self, clazz, name, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (block.isGiven()) {
            Object target = unwrapIfJavaProxy(self);
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod) findCallableArityOne(self, name, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = proc.toJava(paramTypes[0]);
            return method.invokeDirect(context, target, cArg0);
        }
        return call(context, self, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (block.isGiven()) {
            Object target = unwrapIfJavaProxy(self);
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod) findCallableArityTwo(self, name, arg0, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = proc.toJava(paramTypes[1]);
            return method.invokeDirect(context, target, cArg0, cArg1);
        }
        return call(context, self, clazz, name, arg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) {
            Object target = unwrapIfJavaProxy(self);
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod) findCallableArityThree(self, name, arg0, arg1, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = arg1.toJava(paramTypes[1]);
            Object cArg2 = proc.toJava(paramTypes[2]);
            return method.invokeDirect(context, target, cArg0, cArg1, cArg2);
        }
        return call(context, self, clazz, name, arg0, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            Object target = unwrapIfJavaProxy(self);
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod)findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = arg1.toJava(paramTypes[1]);
            Object cArg2 = arg2.toJava(paramTypes[2]);
            Object cArg3 = proc.toJava(paramTypes[3]);
            return method.invokeDirect(context, target, cArg0, cArg1, cArg2, cArg3);
        }
        return call(context, self, clazz, name, arg0, arg1, arg2);
    }
}
