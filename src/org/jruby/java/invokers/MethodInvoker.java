package org.jruby.java.invokers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.javasupport.JavaMethod;

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

    synchronized void createJavaMethods(Ruby runtime) {
        if (!initialized) { // read-volatile
            if (methods != null) {
                if (methods.length == 1) {
                    javaCallable = JavaMethod.create(runtime, methods[0]);
                    if (javaCallable.isVarArgs()) {
                        javaVarargsCallables = new JavaMethod[] {(JavaMethod)javaCallable};
                    }
                } else {
                    Map<Integer, List<JavaMethod>> methodsMap = new HashMap();
                    List<JavaMethod> varargsMethods = new ArrayList();
                    int maxArity = 0;
                    for (Method method: methods) {
                        int arity = method.getParameterTypes().length;
                        maxArity = Math.max(arity, maxArity);
                        List<JavaMethod> methodsForArity = (ArrayList<JavaMethod>)methodsMap.get(arity);
                        if (methodsForArity == null) {
                            methodsForArity = new ArrayList<JavaMethod>();
                            methodsMap.put(arity,methodsForArity);
                        }
                        JavaMethod javaMethod = JavaMethod.create(runtime,method);
                        methodsForArity.add(javaMethod);
                        
                        if (method.isVarArgs()) {
                            minVarargsArity = Math.min(arity - 1, minVarargsArity);
                            varargsMethods.add(javaMethod);
                        }
                    }
                    
                    javaCallables = new JavaMethod[maxArity + 1][];
                    for (Map.Entry<Integer,List<JavaMethod>> entry : methodsMap.entrySet()) {
                        List<JavaMethod> methodsForArity = (List<JavaMethod>)entry.getValue();

                        JavaMethod[] methodsArray = methodsForArity.toArray(new JavaMethod[methodsForArity.size()]);
                        javaCallables[((Integer)entry.getKey()).intValue()] = methodsArray;
                    }

                    if (varargsMethods.size() > 0) {
                        // have at least one varargs, build that map too
                        javaVarargsCallables = new JavaMethod[varargsMethods.size()];
                        varargsMethods.toArray(javaVarargsCallables);
                    }
                }
                methods = null;

                // initialize cache of parameter types to method
                // FIXME: No real reason to use CHM, is there?
                cache = new ConcurrentHashMap(0, 0.75f, 1);
            }
            initialized = true; // write-volatile
        }
    }
}
