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

public class StaticMethodInvoker extends MethodInvoker {
    public StaticMethodInvoker(RubyClass host, List<Method> methods) {
        super(host, methods);
    }

    public StaticMethodInvoker(RubyClass host, Method method) {
        super(host, method);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        createJavaMethods(self.getRuntime());

        int len = args.length;
        Object[] convertedArgs = new Object[len];
        JavaMethod method = (JavaMethod)findCallable(self, name, args, len);
        for (int i = len; --i >= 0;) {
            convertedArgs[i] = convertArg(context, args[i], method, i);
        }
        return method.invokeStaticDirect(convertedArgs);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        createJavaMethods(self.getRuntime());
        JavaMethod method = (JavaMethod)findCallableArityZero(self, name);

        return method.invokeStaticDirect();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        createJavaMethods(self.getRuntime());
        JavaMethod method = (JavaMethod)findCallableArityOne(self, name, arg0);
        Object cArg0 = convertArg(context, arg0, method, 0);

        return method.invokeStaticDirect(cArg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        createJavaMethods(self.getRuntime());
        JavaMethod method = (JavaMethod)findCallableArityTwo(self, name, arg0, arg1);
        Object cArg0 = convertArg(context, arg0, method, 0);
        Object cArg1 = convertArg(context, arg1, method, 1);

        return method.invokeStaticDirect(cArg0, cArg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        createJavaMethods(self.getRuntime());
        JavaMethod method = (JavaMethod)findCallableArityThree(self, name, arg0, arg1, arg2);
        Object cArg0 = convertArg(context, arg0, method, 0);
        Object cArg1 = convertArg(context, arg1, method, 1);
        Object cArg2 = convertArg(context, arg2, method, 2);

        return method.invokeStaticDirect(cArg0, cArg1, cArg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            int len = args.length;
            // too much array creation!
            Object[] convertedArgs = new Object[len + 1];
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallable(self, name, intermediate, len + 1);
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = convertArg(context, intermediate[i], method, i);
            }

            return method.invokeStaticDirect(convertedArgs);
        } else {
            return call(context, self, clazz, name, args);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityOne(self, name, proc);
            Object cArg0 = convertArg(context, proc, method, 0);

            return method.invokeStaticDirect(cArg0);
        } else {
            return call(context, self, clazz, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityTwo(self, name, arg0, proc);
            Object cArg0 = convertArg(context, arg0, method, 0);
            Object cArg1 = convertArg(context, proc, method, 1);

            return method.invokeStaticDirect(cArg0, cArg1);
        } else {
            return call(context, self, clazz, name, arg0);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityThree(self, name, arg0, arg1, proc);
            Object cArg0 = convertArg(context, arg0, method, 0);
            Object cArg1 = convertArg(context, arg1, method, 1);
            Object cArg2 = convertArg(context, proc, method, 2);

            return method.invokeStaticDirect(cArg0, cArg1, cArg2);
        } else {
            return call(context, self, clazz, name, arg0, arg1);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            Object cArg0 = convertArg(context, arg0, method, 0);
            Object cArg1 = convertArg(context, arg1, method, 1);
            Object cArg2 = convertArg(context, arg2, method, 2);
            Object cArg3 = convertArg(context, proc, method, 3);

            return method.invokeStaticDirect(cArg0, cArg1, cArg2, cArg3);
        } else {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }
    }
}
