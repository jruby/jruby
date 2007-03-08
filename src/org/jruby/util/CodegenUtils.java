/*
 * CodegenUtils.java
 *
 * Created on January 31, 2007, 11:54 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.util;

/**
 *
 * @author headius
 */
public class CodegenUtils {
    public static final CodegenUtils instance = new CodegenUtils();
    
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
                } else if (n == Integer.TYPE) {
                    return "[I";
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
                } else if (n == Integer.TYPE) {
                    return "I";
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
     * Create a method signature from the given param types and return values
     */
    public static String sig(Class retval, Class[] params) {
        StringBuffer signature = new StringBuffer("(");
        
        for (int i = 0; i < params.length; i++) {
            signature.append(ci(params[i]));
        }
        
        signature.append(")").append(ci(retval));
        
        return signature.toString();
    }
    
    /**
     * Create a method signature with just a return value
     */
    public static String sig(Class retval) {
        StringBuffer signature = new StringBuffer("()");
        
        signature.append(ci(retval));
        
        return signature.toString();
    }
    
    // TODO: Wouldn't it be nice to replace this all with a single varargs?
    public static Class[] params() {
        return new Class[0];
    }
    public static Class[] params(Class a) {
        return new Class[] {a};
    }
    public static Class[] params(Class a, Class b) {
        return new Class[] {a,b};
    }
    public static Class[] params(Class a, Class b, Class c) {
        return new Class[] {a,b,c};
    }
    public static Class[] params(Class a, Class b, Class c, Class d) {
        return new Class[] {a,b,c,d};
    }
    public static Class[] params(Class a, Class b, Class c, Class d, Class e) {
        return new Class[] {a,b,c,d,e};
    }
    public static Class[] params(Class a, Class b, Class c, Class d, Class e, Class f) {
        return new Class[] {a,b,c,d,e,f};
    }
    public static Class[] params(Class a, Class b, Class c, Class d, Class e, Class f, Class g) {
        return new Class[] {a,b,c,d,e,f,g};
    }
    
}
