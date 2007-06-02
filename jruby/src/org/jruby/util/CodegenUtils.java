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
    public static final CodegenUtils cg = new CodegenUtils();
    
    /**
     * Creates a dotted class name from a path/package name
     */
    public String c(String p) {
        return p.replace('/', '.');
    }

    /**
     * Creates a class path name, from a Class.
     */
    public String p(Class n) {
        return n.getName().replace('.','/');
    }

    /**
     * Creates a class identifier of form Labc/abc;, from a Class.
     */
    public String ci(Class n) {
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
    public String sig(Class retval, Class[] params) {
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
    public String sig(Class retval) {
        StringBuffer signature = new StringBuffer("()");
        
        signature.append(ci(retval));
        
        return signature.toString();
    }
    
    // TODO: Wouldn't it be nice to replace this all with a single varargs?
    public Class[] params() {
        return new Class[0];
    }
    public Class[] params(Class a) {
        return new Class[] {a};
    }
    public Class[] params(Class a, Class b) {
        return new Class[] {a,b};
    }
    public Class[] params(Class a, Class b, Class c) {
        return new Class[] {a,b,c};
    }
    public Class[] params(Class a, Class b, Class c, Class d) {
        return new Class[] {a,b,c,d};
    }
    public Class[] params(Class a, Class b, Class c, Class d, Class e) {
        return new Class[] {a,b,c,d,e};
    }
    public Class[] params(Class a, Class b, Class c, Class d, Class e, Class f) {
        return new Class[] {a,b,c,d,e,f};
    }
    public Class[] params(Class a, Class b, Class c, Class d, Class e, Class f, Class g) {
        return new Class[] {a,b,c,d,e,f,g};
    }
    public Class[] params(Class a, Class b, Class c, Class d, Class e, Class f, Class g, Class h) {
        return new Class[] {a,b,c,d,e,f,g,h};
    }
    public Class[] params(Class a, Class b, Class c, Class d, Class e, Class f, Class g, Class h, Class i) {
        return new Class[] {a,b,c,d,e,f,g,h,i}; 
    }
    
    public String cleanJavaIdentifier(String name) {
        char[] characters = name.toCharArray();
        StringBuffer cleanBuffer = new StringBuffer();
        boolean prevWasReplaced = false;
        for (int i = 0; i < characters.length; i++) {
            if (Character.isJavaIdentifierStart(characters[i])) {
                cleanBuffer.append(characters[i]);
                prevWasReplaced = false;
            } else {
                if (!prevWasReplaced) {
                    cleanBuffer.append("_");
                }
                prevWasReplaced = true;
                switch (characters[i]) {
                case '?':
                    cleanBuffer.append("p_");
                    continue;
                case '!':
                    cleanBuffer.append("b_");
                    continue;
                case '<':
                    cleanBuffer.append("lt_");
                    continue;
                case '>':
                    cleanBuffer.append("gt_");
                    continue;
                case '=':
                    cleanBuffer.append("equal_");
                    continue;
                case '[':
                    if ((i + 1) < characters.length && characters[i + 1] == ']') {
                        cleanBuffer.append("aref_");
                        i++;
                    } else {
                        // can this ever happen?
                        cleanBuffer.append("lbracket_");
                    }
                    continue;
                case ']':
                    // given [ logic above, can this ever happen?
                    cleanBuffer.append("rbracket_");
                    continue;
                case '+':
                    cleanBuffer.append("plus_");
                    continue;
                case '-':
                    cleanBuffer.append("minus_");
                    continue;
                case '*':
                    cleanBuffer.append("times_");
                    continue;
                case '/':
                    cleanBuffer.append("div_");
                    continue;
                case '&':
                    cleanBuffer.append("and_");
                    continue;
                default:
                    cleanBuffer.append(Integer.toHexString(characters[i])).append("_");
                }
            }
        }
        return cleanBuffer.toString();
    }
}
