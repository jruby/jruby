package org.jruby.javasupport.methods;

import org.jruby.javasupport.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class MethodInvoker extends org.jruby.internal.runtime.methods.JavaMethod {
    protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private Method[] methods;
    protected JavaMethod javaMethod;
    protected JavaMethod[][] javaMethods;
    protected Map cache;
    protected volatile boolean initialized;
    
    MethodInvoker(RubyClass host, List<Method> methods) {
        super(host, Visibility.PUBLIC);
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
                    javaMethod = JavaMethod.create(runtime, methods[0]);
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
                    javaMethods = new JavaMethod[maxArity + 1][];
                    for (Iterator<Map.Entry> iter = methodsMap.entrySet().iterator(); iter.hasNext();) {
                        Map.Entry entry = iter.next();
                        List<JavaMethod> methodsForArity = (List<JavaMethod>)entry.getValue();

                        JavaMethod[] methodsArray = methodsForArity.toArray(new JavaMethod[methodsForArity.size()]);
                        javaMethods[((Integer)entry.getKey()).intValue()] = methodsArray;
                    }
                }
                methods = null;

                // initialize cache of parameter types to method
                cache = new ConcurrentHashMap();
            }
            initialized = true; // write-volatile
        }
    }

    void raiseNoMatchingMethodError(String name, IRubyObject proxy, Object... args) {
        int len = args.length;
        Class[] argTypes = new Class[args.length];
        for (int i = 0; i < len; i++) {
            argTypes[i] = args[i].getClass();
        }
        throw proxy.getRuntime().newNameError("no " + name + " with arguments matching " + Arrays.toString(argTypes) + " on object " + proxy.callMethod(proxy.getRuntime().getCurrentContext(),"inspect"), null);
    }

    protected JavaMethod findMethod(IRubyObject self, String name, IRubyObject[] args, int arity) {
        JavaMethod method;
        if ((method = javaMethod) == null) {
            // TODO: varargs?
            JavaMethod[] methodsForArity = null;
            if (arity > javaMethods.length || (methodsForArity = javaMethods[arity]) == null) {
                raiseNoMatchingMethodError(name, self, args, 0);
            }
            method = (JavaMethod)Java.matchingMethodArityN(self, cache, methodsForArity, args, arity);
        }
        return method;
    }

    protected JavaMethod findMethodArityZero(IRubyObject self, String name) {
        JavaMethod method;
        if ((method = javaMethod) == null) {
            // TODO: varargs?
            JavaMethod[] methodsForArity = null;
            if (javaMethods.length == 0 || (methodsForArity = javaMethods[0]) == null) {
                raiseNoMatchingMethodError(name, self, IRubyObject.NULL_ARRAY, 0);
            }
            method = methodsForArity[0];
        }
        return method;
    }

    protected JavaMethod findMethodArityOne(IRubyObject self, String name, IRubyObject arg0) {
        JavaMethod method;
        if ((method = javaMethod) == null) {
            // TODO: varargs?
            JavaMethod[] methodsForArity = null;
            if (javaMethods.length < 1 || (methodsForArity = javaMethods[1]) == null) {
                raiseNoMatchingMethodError(name, self, arg0);
            }
            method = (JavaMethod)Java.matchingMethodArityOne(self, cache, methodsForArity, arg0);
        }
        return method;
    }

    protected JavaMethod findMethodArityTwo(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        JavaMethod method;
        if ((method = javaMethod) == null) {
            // TODO: varargs?
            JavaMethod[] methodsForArity = null;
            if (javaMethods.length <= 2 || (methodsForArity = javaMethods[2]) == null) {
                raiseNoMatchingMethodError(name, self, arg0, arg1);
            }
            method = (JavaMethod)Java.matchingMethodArityTwo(self, cache, methodsForArity, arg0, arg1);
        }
        return method;
    }

    protected JavaMethod findMethodArityThree(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        JavaMethod method;
        if ((method = javaMethod) == null) {
            // TODO: varargs?
            JavaMethod[] methodsForArity = null;
            if (javaMethods.length <= 3 || (methodsForArity = javaMethods[3]) == null) {
                raiseNoMatchingMethodError(name, self, arg0, arg1, arg2);
            }
            method = (JavaMethod)Java.matchingMethodArityThree(self, cache, methodsForArity, arg0, arg1, arg2);
        }
        return method;
    }

    protected JavaMethod findMethodArityFour(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        JavaMethod method;
        if ((method = javaMethod) == null) {
            // TODO: varargs?
            JavaMethod[] methodsForArity = null;
            if (javaMethods.length <= 4 || (methodsForArity = javaMethods[4]) == null) {
                raiseNoMatchingMethodError(name, self, arg0, arg1, arg2, arg3);
            }
            method = (JavaMethod)Java.matchingMethodArityFour(self, cache, methodsForArity, arg0, arg1, arg2, arg3);
        }
        return method;
    }
}