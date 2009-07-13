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
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstructorInvoker extends RubyToJavaInvoker {
    private Constructor[] ctors;
    
    public ConstructorInvoker(RubyModule host, List<Constructor> methods) {
        super(host);
        this.ctors = methods.toArray(new Constructor[methods.size()]);
        
        if (!Ruby.isSecurityRestricted()) {
            try {
                Method.setAccessible(this.ctors, true);
            } catch(SecurityException e) {}
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
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        createJavaCallables(runtime);
        JavaProxy proxy = castJavaProxy(self);

        int len = args.length;
        Object[] convertedArgs = new Object[len];
        JavaConstructor constructor = (JavaConstructor)findCallable(self, name, args, len);
        for (int i = 0; i < len; i++) {
            convertedArgs[i] = convertArg(context, args[i], constructor, i);
        }
        
        proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(convertedArgs)));
        
        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        Ruby runtime = context.getRuntime();
        createJavaCallables(runtime);
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor)findCallableArityZero(self, name);

        proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect()));
        
        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        Ruby runtime = context.getRuntime();
        createJavaCallables(runtime);
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor)findCallableArityOne(self, name, arg0);
        Object cArg0 = convertArg(context, arg0, constructor, 0);

        proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(cArg0)));
        
        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.getRuntime();
        createJavaCallables(runtime);
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor)findCallableArityTwo(self, name, arg0, arg1);
        Object cArg0 = convertArg(context, arg0, constructor, 0);
        Object cArg1 = convertArg(context, arg1, constructor, 1);

        proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(cArg0, cArg1)));

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.getRuntime();
        createJavaCallables(runtime);
        JavaProxy proxy = castJavaProxy(self);
        JavaConstructor constructor = (JavaConstructor)findCallableArityThree(self, name, arg0, arg1, arg2);
        Object cArg0 = convertArg(context, arg0, constructor, 0);
        Object cArg1 = convertArg(context, arg1, constructor, 1);
        Object cArg2 = convertArg(context, arg2, constructor, 2);

        proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(cArg0, cArg1, cArg2)));

        return self;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.getRuntime();
            createJavaCallables(runtime);
            JavaProxy proxy = castJavaProxy(self);
            
            int len = args.length;
            // too much array creation!
            Object[] convertedArgs = new Object[len + 1];
            IRubyObject[] intermediate = new IRubyObject[len + 1];
            System.arraycopy(args, 0, intermediate, 0, len);
            intermediate[len] = RubyProc.newProc(runtime, block, Block.Type.LAMBDA);
            JavaConstructor constructor = (JavaConstructor)findCallable(self, name, intermediate, len + 1);
            for (int i = 0; i < len + 1; i++) {
                convertedArgs[i] = convertArg(context, intermediate[i], constructor, i);
            }

            proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(convertedArgs)));

            return self;
        } else {
            return call(context, self, clazz, name, args);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.getRuntime();
            createJavaCallables(runtime);
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor constructor = (JavaConstructor)findCallableArityOne(self, name, proc);
            Object cArg0 = convertArg(context, proc, constructor, 0);

            proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(cArg0)));

            return self;
        } else {
            return call(context, self, clazz, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.getRuntime();
            createJavaCallables(runtime);
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor constructor = (JavaConstructor)findCallableArityTwo(self, name, arg0, proc);
            Object cArg0 = convertArg(context, arg0, constructor, 0);
            Object cArg1 = convertArg(context, proc, constructor, 1);

            proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(cArg0, cArg1)));

            return self;
        } else {
            return call(context, self, clazz, name, arg0);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.getRuntime();
            createJavaCallables(runtime);
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor constructor = (JavaConstructor)findCallableArityThree(self, name, arg0, arg1, proc);
            Object cArg0 = convertArg(context, arg0, constructor, 0);
            Object cArg1 = convertArg(context, arg1, constructor, 1);
            Object cArg2 = convertArg(context, proc, constructor, 2);

            proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(cArg0, cArg1, cArg2)));

            return self;
        } else {
            return call(context, self, clazz, name, arg0, arg1);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.getRuntime();
            createJavaCallables(runtime);
            JavaProxy proxy = castJavaProxy(self);

            RubyProc proc = RubyProc.newProc(self.getRuntime(), block, Block.Type.LAMBDA);
            JavaConstructor constructor = (JavaConstructor)findCallableArityFour(self, name, arg0, arg1, arg2, proc);
            Object cArg0 = convertArg(context, arg0, constructor, 0);
            Object cArg1 = convertArg(context, arg1, constructor, 1);
            Object cArg2 = convertArg(context, arg2, constructor, 2);
            Object cArg3 = convertArg(context, proc, constructor, 3);

            proxy.dataWrapStruct(JavaObject.wrap(runtime, constructor.newInstanceDirect(cArg0, cArg1, cArg2, cArg3)));

            return self;
        } else {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }
    }
}
