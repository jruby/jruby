package org.jruby.java.invokers;

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

public abstract class MethodInvoker extends RubyToJavaInvoker {
    private Method[] methods;
    
    MethodInvoker(RubyModule host, List<Method> methods) {
        super(host);
        this.methods = methods.toArray(new Method[methods.size()]);
        
        if (!Ruby.isSecurityRestricted()) {
            Method.setAccessible(this.methods, true);
        }
    }

    // TODO: varargs?
    synchronized void createJavaMethods(Ruby runtime) {
        if (!initialized) { // read-volatile
            if (methods != null) {
                if (methods.length == 1) {
                    javaCallable = JavaMethod.create(runtime, methods[0]);
                } else {
                    Map methodsMap = new HashMap();
                    int maxArity = 0;
                    for (Method method: methods) {
                        // TODO: deal with varargs
                        int arity = method.getParameterTypes().length;
                        maxArity = Math.max(arity, maxArity);
                        List<JavaMethod> methodsForArity = (ArrayList<JavaMethod>)methodsMap.get(arity);
                        if (methodsForArity == null) {
                            methodsForArity = new ArrayList<JavaMethod>();
                            methodsMap.put(arity,methodsForArity);
                        }
                        methodsForArity.add(JavaMethod.create(runtime,method));
                    }
                    javaCallables = new JavaMethod[maxArity + 1][];
                    for (Iterator<Map.Entry> iter = methodsMap.entrySet().iterator(); iter.hasNext();) {
                        Map.Entry entry = iter.next();
                        List<JavaMethod> methodsForArity = (List<JavaMethod>)entry.getValue();

                        JavaMethod[] methodsArray = methodsForArity.toArray(new JavaMethod[methodsForArity.size()]);
                        javaCallables[((Integer)entry.getKey()).intValue()] = methodsArray;
                    }
                }
                methods = null;

                // initialize cache of parameter types to method
                cache = new ConcurrentHashMap();
            }
            initialized = true; // write-volatile
        }
    }
}