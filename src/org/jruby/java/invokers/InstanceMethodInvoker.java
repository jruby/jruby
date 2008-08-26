package org.jruby.java.invokers;

import java.lang.reflect.Method;
import java.util.List;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaMethod;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceMethodInvoker extends MethodInvoker {

    public InstanceMethodInvoker(RubyModule host, List<Method> methods) {
        super(host, methods);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        createJavaMethods(self.getRuntime());

        int len = args.length;
        Object[] convertedArgs = new Object[len];
        JavaMethod method = (JavaMethod)findCallable(self, name, args, len);
        for (int i = 0; i < len; i++) {
            convertedArgs[i] = JavaUtil.convertArgumentToType(context, args[i], method.getParameterTypes()[i]);
        }
        return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        createJavaMethods(self.getRuntime());
        JavaMethod method = (JavaMethod)findCallableArityZero(self, name);
        return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), EMPTY_OBJECT_ARRAY), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        createJavaMethods(self.getRuntime());
        Object[] convertedArgs = new Object[1];
        JavaMethod method = (JavaMethod)findCallableArityOne(self, name, arg0);
        convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
        return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        createJavaMethods(self.getRuntime());

        int len = 2;
        Object[] convertedArgs = new Object[len];
        JavaMethod method = (JavaMethod)findCallableArityTwo(self, name, arg0, arg1);
        convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
        convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, method.getParameterTypes()[1]);
        return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        createJavaMethods(self.getRuntime());

        int len = 3;
        Object[] convertedArgs = new Object[len];
        JavaMethod method = (JavaMethod)findCallableArityThree(self, name, arg0, arg1, arg2);
        convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
        convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, method.getParameterTypes()[1]);
        convertedArgs[2] = JavaUtil.convertArgumentToType(context, arg2, method.getParameterTypes()[2]);
        return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            int len = args.length;
            Object[] convertedArgs = new Object[len + 1];
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallable(self, name, intermediate, len + 1);
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = JavaUtil.convertArgumentToType(context, intermediate[i], method.getParameterTypes()[i]);
            }
            return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, args);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[1];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityOne(self, name, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, proc, method.getParameterTypes()[0]);
            return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[2];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityTwo(self, name, arg0, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
            convertedArgs[1] = JavaUtil.convertArgumentToType(context, proc, method.getParameterTypes()[1]);
            return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, arg0);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[3];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityThree(self, name, arg0, arg1, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
            convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, method.getParameterTypes()[1]);
            convertedArgs[2] = JavaUtil.convertArgumentToType(context, proc, method.getParameterTypes()[2]);
            return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, arg0, arg1);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        createJavaMethods(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[4];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaMethod method = (JavaMethod)findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
            convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, method.getParameterTypes()[1]);
            convertedArgs[2] = JavaUtil.convertArgumentToType(context, arg2, method.getParameterTypes()[2]);
            convertedArgs[3] = JavaUtil.convertArgumentToType(context, proc, method.getParameterTypes()[3]);
            return Java.java_to_ruby(self, method.invoke((JavaObject) self.dataGetStruct(), convertedArgs), Block.NULL_BLOCK);
        } else {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }
    }
}
