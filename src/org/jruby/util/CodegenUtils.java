/*
 * CodegenUtils.java
 *
 * Created on January 31, 2007, 11:54 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.util;

import java.util.Arrays;

/**
 *
 * @author headius
 */
public class CodegenUtils {
    /**
     * Creates a dotted class name from a path/package name
     */
    public static String c(String p) {
        return p.replace('/', '.');
    }

    /**
     * Creates a class path name, from a Class.
     */
    public static String p(Class n) {
        return n.getName().replace('.','/');
    }

    /**
     * Creates a class identifier of form Labc/abc;, from a Class.
     */
    public static String ci(Class n) {
        if (n.isArray()) {
            n = n.getComponentType();
            if (n.isPrimitive()) {
                if (n == Byte.TYPE) {
                    return "[B";
                } else if (n == Boolean.TYPE) {
                    return "[Z";
                } else if (n == Short.TYPE) {
                    return "[S";
                } else if (n == Character.TYPE) {
                    return "[C";
                } else if (n == Integer.TYPE) {
                    return "[I";
                } else if (n == Float.TYPE) {
                    return "[F";
                } else if (n == Double.TYPE) {
                    return "[D";
                } else if (n == Long.TYPE) {
                    return "[J";
                } else {
                    throw new RuntimeException("Unrecognized type in compiler: " + n.getName());
                }
            } else {
                return "[" + ci(n);
            }
        } else {
            if (n.isPrimitive()) {
                if (n == Byte.TYPE) {
                    return "B";
                } else if (n == Boolean.TYPE) {
                    return "Z";
                } else if (n == Short.TYPE) {
                    return "S";
                } else if (n == Character.TYPE) {
                    return "C";
                } else if (n == Integer.TYPE) {
                    return "I";
                } else if (n == Float.TYPE) {
                    return "F";
                } else if (n == Double.TYPE) {
                    return "D";
                } else if (n == Long.TYPE) {
                    return "J";
                } else if (n == Void.TYPE) {
                    return "V";
                } else {
                    throw new RuntimeException("Unrecognized type in compiler: " + n.getName());
                }
            } else {
                return "L" + p(n) + ";";
            }
        }
    }

    /**
     * Creates a human-readable representation, from a Class.
     */
    public static String human(Class n) {
        return n.getCanonicalName();
    }
    
    /**
     * Create a method signature from the given param types and return values
     */
    public static String sig(Class retval, Class... params) {
        return sigParams(params) + ci(retval);
    }
    
    public static String sigParams(Class... params) {
        StringBuilder signature = new StringBuilder("(");
        
        for (int i = 0; i < params.length; i++) {
            signature.append(ci(params[i]));
        }
        
        signature.append(")");
        
        return signature.toString();
    }
    
    public static String pretty(Class retval, Class... params) {
        return prettyParams(params) + human(retval);
    }
    
    public static String prettyParams(Class... params) {
        StringBuilder signature = new StringBuilder("(");
        
        for (int i = 0; i < params.length; i++) {
            signature.append(human(params[i]));
            if (i < params.length - 1) signature.append(',');
        }
        
        signature.append(")");
        
        return signature.toString();
    }
    
    public static Class[] params(Class... classes) {
        return classes;
    }
    
    public static Class[] params(Class cls, int times) {
        Class[] classes = new Class[times];
        Arrays.fill(classes, cls);
        return classes;
    }
    
    public static Class[] params(Class cls1, Class clsFill, int times) {
        Class[] classes = new Class[times + 1];
        Arrays.fill(classes, clsFill);
        classes[0] = cls1;
        return classes;
    }
    
    public static String getAnnotatedBindingClassName(String javaMethodName, String typeName, boolean isStatic, int required, int optional, boolean multi, boolean framed) {
        String commonClassSuffix;
        String marker = framed ? "$RUBYFRAMEDINVOKER$" : "$RUBYINVOKER$";
        if (multi) {
            commonClassSuffix = (isStatic ? "$s" : "$i" ) + "_method_multi" + marker + javaMethodName;
        } else {
            commonClassSuffix = (isStatic ? "$s" : "$i" ) + "_method_" + required + "_" + optional + marker + javaMethodName;
        }
        return typeName + commonClassSuffix;
    }
}
