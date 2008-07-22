package org.jruby.javasupport.methods;

import org.jruby.javasupport.*;
import java.lang.reflect.Method;
import java.util.List;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StaticMethodInvoker extends MethodInvoker {

    public StaticMethodInvoker(RubyClass host, List<Method> methods) {
        super(host, methods);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        createJavaMethods(self.getRuntime());

        int len = args.length;
        Object[] convertedArgs = new Object[len];
        JavaMethod method = findMethod(self, name, args, len);
        Class[] targetTypes = method.getParameterTypes();
        for (int i = len; --i >= 0;) {
            convertedArgs[i] = JavaClass.convertArgumentToType(context, args[i], targetTypes[i]);
        }
        return Java.java_to_ruby(self, method.invoke_static(convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        createJavaMethods(self.getRuntime());
        JavaMethod method = findMethodArityZero(self, name);

        return Java.java_to_ruby(self, method.invoke_static(EMPTY_OBJECT_ARRAY), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        createJavaMethods(self.getRuntime());
        Object[] convertedArgs = new Object[1];
        JavaMethod method = findMethodArityOne(self, name, arg0);
        convertedArgs[0] = JavaClass.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);

        return Java.java_to_ruby(self, method.invoke_static(convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        createJavaMethods(self.getRuntime());
        Object[] convertedArgs = new Object[2];
        JavaMethod method = findMethodArityTwo(self, name, arg0, arg1);
        convertedArgs[0] = JavaClass.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
        convertedArgs[1] = JavaClass.convertArgumentToType(context, arg1, method.getParameterTypes()[1]);

        return Java.java_to_ruby(self, method.invoke_static(convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        createJavaMethods(self.getRuntime());
        Object[] convertedArgs = new Object[3];
        JavaMethod method = findMethodArityThree(self, name, arg0, arg1, arg2);
        convertedArgs[0] = JavaClass.convertArgumentToType(context, arg0, method.getParameterTypes()[0]);
        convertedArgs[1] = JavaClass.convertArgumentToType(context, arg1, method.getParameterTypes()[1]);
        convertedArgs[2] = JavaClass.convertArgumentToType(context, arg2, method.getParameterTypes()[2]);

        return Java.java_to_ruby(self, method.invoke_static(convertedArgs), Block.NULL_BLOCK);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        return call(context, self, clazz, name, args);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        return call(context, self, clazz, name);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        return call(context, self, clazz, name, arg0);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return call(context, self, clazz, name, arg0, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return call(context, self, clazz, name, arg0, arg1, arg2);
    }
}
