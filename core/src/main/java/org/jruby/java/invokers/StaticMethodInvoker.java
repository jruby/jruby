package org.jruby.java.invokers;

import java.lang.reflect.Method;
import java.util.List;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.javasupport.JavaMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public final class StaticMethodInvoker extends MethodInvoker {

    public StaticMethodInvoker(RubyClass host, List<Method> methods) {
        super(host, methods);
    }

    public StaticMethodInvoker(RubyClass host, Method method) {
        super(host, method);
    }

    public StaticMethodInvoker(RubyModule host, Method method) {
        super(host, method);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        JavaMethod method = (JavaMethod) findCallable(self, name, args, args.length);
        return method.invokeStaticDirect( context, convertArguments(method, args) );
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY);
        JavaMethod method = (JavaMethod) findCallableArityZero(self, name);

        return method.invokeStaticDirect(context);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0});
        JavaMethod method = (JavaMethod) findCallableArityOne(self, name, arg0);
        if (method.isVarArgs()) return call(context, self, clazz, name, new IRubyObject[] {arg0});
        final Class<?>[] paramTypes = method.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);

        return method.invokeStaticDirect(context, cArg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1});
        JavaMethod method = (JavaMethod) findCallableArityTwo(self, name, arg0, arg1);
        final Class<?>[] paramTypes = method.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);
        Object cArg1 = arg1.toJava(paramTypes[1]);

        return method.invokeStaticDirect(context, cArg0, cArg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (javaVarargsCallables != null) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2});
        JavaMethod method = (JavaMethod) findCallableArityThree(self, name, arg0, arg1, arg2);
        final Class<?>[] paramTypes = method.getParameterTypes();
        Object cArg0 = arg0.toJava(paramTypes[0]);
        Object cArg1 = arg1.toJava(paramTypes[1]);
        Object cArg2 = arg2.toJava(paramTypes[2]);

        return method.invokeStaticDirect(context, cArg0, cArg1, cArg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            int len = args.length;
            // too much array creation!
            Object[] convertedArgs = new Object[len + 1];
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(context.runtime, block, block.type);

            JavaMethod method = (JavaMethod) findCallable(self, name, intermediate, len + 1);
            final Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = intermediate[i].toJava(paramTypes[i]);
            }

            return method.invokeStaticDirect(context, convertedArgs);
        }
        return call(context, self, clazz, name, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod) findCallableArityOne(self, name, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = proc.toJava(paramTypes[0]);

            return method.invokeStaticDirect(context, cArg0);
        }
        return call(context, self, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod) findCallableArityTwo(self, name, arg0, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = proc.toJava(paramTypes[1]);

            return method.invokeStaticDirect(context, cArg0, cArg1);
        }
        return call(context, self, clazz, name, arg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod) findCallableArityThree(self, name, arg0, arg1, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = arg1.toJava(paramTypes[1]);
            Object cArg2 = proc.toJava(paramTypes[2]);

            return method.invokeStaticDirect(context, cArg0, cArg1, cArg2);

        }
        return call(context, self, clazz, name, arg0, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            RubyProc proc = RubyProc.newProc(context.runtime, block, block.type);
            JavaMethod method = (JavaMethod) findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            final Class<?>[] paramTypes = method.getParameterTypes();
            Object cArg0 = arg0.toJava(paramTypes[0]);
            Object cArg1 = arg1.toJava(paramTypes[1]);
            Object cArg2 = arg2.toJava(paramTypes[2]);
            Object cArg3 = proc.toJava(paramTypes[3]);

            return method.invokeStaticDirect(context, cArg0, cArg1, cArg2, cArg3);
        }
        return call(context, self, clazz, name, arg0, arg1, arg2);
    }
}
