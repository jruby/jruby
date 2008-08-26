package org.jruby.java.invokers;

import java.lang.reflect.Constructor;
import org.jruby.javasupport.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstructorInvoker extends RubyToJavaInvoker {
    private Constructor[] ctors;
    
    public ConstructorInvoker(RubyModule host, List<Constructor> methods) {
        super(host);
        this.ctors = methods.toArray(new Constructor[methods.size()]);
        
        if (!Ruby.isSecurityRestricted()) {
            Method.setAccessible(this.ctors, true);
        }
    }

    // TODO: varargs?
    synchronized void createJavaCallables(Ruby runtime) {
        if (!initialized) { // read-volatile
            if (ctors != null) {
                if (ctors.length == 1) {
                    javaCallable = JavaConstructor.create(runtime, ctors[0]);
                } else {
                    Map methodsMap = new HashMap();
                    int maxArity = 0;
                    for (Constructor ctor: ctors) {
                        // TODO: deal with varargs
                        int arity = ctor.getParameterTypes().length;
                        maxArity = Math.max(arity, maxArity);
                        List<JavaConstructor> methodsForArity = (ArrayList<JavaConstructor>)methodsMap.get(arity);
                        if (methodsForArity == null) {
                            methodsForArity = new ArrayList<JavaConstructor>();
                            methodsMap.put(arity,methodsForArity);
                        }
                        methodsForArity.add(JavaConstructor.create(runtime,ctor));
                    }
                    javaCallables = new JavaConstructor[maxArity + 1][];
                    for (Iterator<Map.Entry> iter = methodsMap.entrySet().iterator(); iter.hasNext();) {
                        Map.Entry entry = iter.next();
                        List<JavaConstructor> ctorsForArity = (List<JavaConstructor>)entry.getValue();

                        JavaConstructor[] methodsArray = ctorsForArity.toArray(new JavaConstructor[ctorsForArity.size()]);
                        javaCallables[((Integer)entry.getKey()).intValue()] = methodsArray;
                    }
                }
                ctors = null;

                // initialize cache of parameter types to method
                cache = new ConcurrentHashMap();
            }
            initialized = true; // write-volatile
        }
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        createJavaCallables(self.getRuntime());

        int len = args.length;
        Object[] convertedArgs = new Object[len];
        JavaConstructor callable = (JavaConstructor)findCallable(self, name, args, len);
        Class[] targetTypes = callable.getParameterTypes();
        for (int i = len; --i >= 0;) {
            convertedArgs[i] = JavaUtil.convertArgumentToType(context, args[i], targetTypes[i]);
        }
        
        Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));
        
        return self;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        createJavaCallables(self.getRuntime());
        JavaConstructor callable = (JavaConstructor)findCallableArityZero(self, name);
        
        Java.JavaUtilities.set_java_object(self, self, callable.new_instance(EMPTY_OBJECT_ARRAY));
        
        return self;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        createJavaCallables(self.getRuntime());
        Object[] convertedArgs = new Object[1];
        JavaConstructor callable = (JavaConstructor)findCallableArityOne(self, name, arg0);
        convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, callable.getParameterTypes()[0]);
        
        Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));
        
        return self;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        createJavaCallables(self.getRuntime());
        Object[] convertedArgs = new Object[2];
        JavaConstructor callable = (JavaConstructor)findCallableArityTwo(self, name, arg0, arg1);
        convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, callable.getParameterTypes()[0]);
        convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, callable.getParameterTypes()[1]);
        
        Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));
        
        return self;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        createJavaCallables(self.getRuntime());
        Object[] convertedArgs = new Object[3];
        JavaConstructor callable = (JavaConstructor)findCallableArityThree(self, name, arg0, arg1, arg2);
        convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, callable.getParameterTypes()[0]);
        convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, callable.getParameterTypes()[1]);
        convertedArgs[2] = JavaUtil.convertArgumentToType(context, arg2, callable.getParameterTypes()[2]);
        
        Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));
        
        return self;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        createJavaCallables(self.getRuntime());
        if (block.isGiven()) {
            int len = args.length;
            Object[] convertedArgs = new Object[len + 1];
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor callable = (JavaConstructor)findCallable(self, name, intermediate, len + 1);
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = JavaUtil.convertArgumentToType(context, intermediate[i], callable.getParameterTypes()[i]);
            }
        
            Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));

            return self;
        } else {
            return call(context, self, clazz, name, args);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        createJavaCallables(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[1];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor callable = (JavaConstructor)findCallableArityOne(self, name, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, proc, callable.getParameterTypes()[0]);
        
            Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));

            return self;
        } else {
            return call(context, self, clazz, name);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        createJavaCallables(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[2];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor callable = (JavaConstructor)findCallableArityTwo(self, name, arg0, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, callable.getParameterTypes()[0]);
            convertedArgs[1] = JavaUtil.convertArgumentToType(context, proc, callable.getParameterTypes()[1]);
        
            Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));

            return self;
        } else {
            return call(context, self, clazz, name, arg0);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        createJavaCallables(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[3];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block
                    .Type.LAMBDA);
            JavaConstructor callable = (JavaConstructor)findCallableArityThree(self, name, arg0, arg1, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, callable.getParameterTypes()[0]);
            convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, callable.getParameterTypes()[1]);
            convertedArgs[2] = JavaUtil.convertArgumentToType(context, proc, callable.getParameterTypes()[2]);
        
            Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));

            return self;
        } else {
            return call(context, self, clazz, name, arg0, arg1);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        createJavaCallables(self.getRuntime());
        if (block.isGiven()) {
            Object[] convertedArgs = new Object[4];
            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor callable = (JavaConstructor)findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            convertedArgs[0] = JavaUtil.convertArgumentToType(context, arg0, callable.getParameterTypes()[0]);
            convertedArgs[1] = JavaUtil.convertArgumentToType(context, arg1, callable.getParameterTypes()[1]);
            convertedArgs[2] = JavaUtil.convertArgumentToType(context, arg2, callable.getParameterTypes()[2]);
            convertedArgs[3] = JavaUtil.convertArgumentToType(context, proc, callable.getParameterTypes()[3]);
        
            Java.JavaUtilities.set_java_object(self, self, callable.new_instance(convertedArgs));

            return self;
        } else {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }
    }
}