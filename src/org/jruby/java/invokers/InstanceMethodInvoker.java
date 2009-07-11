package org.jruby.java.invokers;

import java.lang.reflect.Method;
import java.util.List;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceMethodInvoker extends MethodInvoker {

    public InstanceMethodInvoker(RubyModule host, List<Method> methods) {
        super(host, methods);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        createJavaMethods(context.getRuntime());
        JavaProxy proxy = castJavaProxy(self);

        int len = args.length;
        Object[] convertedArgs = new Object[len];
        JavaMethod method = (JavaMethod)findCallable(self, name, args, len);
        for (int i = 0; i < len; i++) {
            convertedArgs[i] = convertArg(context, args[i], method, i);
        }
        return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), convertedArgs), Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        createJavaMethods(self.getRuntime());
        JavaProxy proxy = castJavaProxy(self);
        JavaMethod method = (JavaMethod)findCallableArityZero(self, name);
        return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject()), Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        createJavaMethods(self.getRuntime());
        JavaProxy proxy = castJavaProxy(self);
        JavaMethod method = (JavaMethod)findCallableArityOne(self, name, arg0);
        Object cArg0 = convertArg(context, arg0, method, 0);
        return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), cArg0), Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        createJavaMethods(self.getRuntime());
        JavaProxy proxy = castJavaProxy(self);
        JavaMethod method = (JavaMethod)findCallableArityTwo(self, name, arg0, arg1);
        Object cArg0 = convertArg(context, arg0, method, 0);
        Object cArg1 = convertArg(context, arg1, method, 1);
        return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), cArg0, cArg1), Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        createJavaMethods(self.getRuntime());
        JavaProxy proxy = castJavaProxy(self);
        JavaMethod method = (JavaMethod)findCallableArityThree(self, name, arg0, arg1, arg2);
        Object cArg0 = convertArg(context, arg0, method, 0);
        Object cArg1 = convertArg(context, arg1, method, 1);
        Object cArg2 = convertArg(context, arg2, method, 2);
        return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), cArg0, cArg1, cArg2), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            JavaProxy proxy = castJavaProxy(self);
            int len = args.length;
            // these extra arrays are really unfortunate; split some of these paths out to eliminate?
            Object[] convertedArgs = new Object[len + 1];
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallable(self, name, intermediate, len + 1);
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = convertArg(context, intermediate[i], method, i);
            }
            return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), convertedArgs), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, args);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            JavaProxy proxy = castJavaProxy(self);
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityOne(self, name, proc);
            Object cArg0 = convertArg(context, proc, method, 0);
            return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), cArg0), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            JavaProxy proxy = castJavaProxy(self);
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityTwo(self, name, arg0, proc);
            Object cArg0 = convertArg(context, arg0, method, 0);
            Object cArg1 = convertArg(context, proc, method, 1);
            return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), cArg0, cArg1), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, arg0);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            JavaProxy proxy = castJavaProxy(self);
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityThree(self, name, arg0, arg1, proc);
            Object cArg0 = convertArg(context, arg0, method, 0);
            Object cArg1 = convertArg(context, arg1, method, 1);
            Object cArg2 = convertArg(context, proc, method, 2);
            return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), cArg0, cArg1, cArg2), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, arg0, arg1);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            createJavaMethods(self.getRuntime());
            JavaProxy proxy = castJavaProxy(self);
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            Object cArg0 = convertArg(context, arg0, method, 0);
            Object cArg1 = convertArg(context, arg1, method, 1);
            Object cArg2 = convertArg(context, arg2, method, 2);
            Object cArg3 = convertArg(context, proc, method, 3);
            return Java.java_to_ruby(self, method.invokeDirect(proxy.getObject(), cArg0, cArg1, cArg2, cArg3), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }
    }

    static JavaProxy castJavaProxy(IRubyObject self) throws RaiseException {
        if (!(self instanceof JavaProxy)) {
            throw self.getRuntime().newTypeError("Java methods can only be invoked on Java objects");
        }
        JavaProxy proxy = (JavaProxy)self;
        return proxy;
    }
}
