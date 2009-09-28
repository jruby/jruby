package org.jruby.java.invokers;

import org.jruby.javasupport.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        trySetAccessible(this.methods);
    }

    MethodInvoker(RubyModule host, Method method) {
        super(host);
        this.methods = new Method[] {method};
        trySetAccessible(methods);
    }

    private static void trySetAccessible(Method[] methods) {
        if (!Ruby.isSecurityRestricted()) {
            try {
                Method.setAccessible(methods, true);
            } catch(SecurityException e) {}
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
