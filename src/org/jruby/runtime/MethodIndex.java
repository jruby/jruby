/*
 * MethodIndex.java
 *
 * Created on January 1, 2007, 7:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

/**
 *
 * @author headius
 */
public class MethodIndex {
    public static final byte NO_INDEX = 0;
    public static final byte OP_PLUS = 1;
    public static final byte OP_MINUS = 2;
    public static final byte OP_LT = 3;
    public static final byte AREF = 4;
    public static final byte ASET = 5;
    public static final byte POP = 6;
    public static final byte PUSH = 7;
    public static final byte NIL_P = 8;
    public static final byte EQUALEQUAL = 9;
    public static final byte UNSHIFT = 10;
    public static final byte OP_GE = 11;
    public static final byte OP_LSHIFT = 12;
    public static final byte EMPTY_P = 13;
    public static final byte MAX_METHODS = 14;
    
    /** Creates a new instance of MethodIndex */
    public MethodIndex() {
    }
    
    public static byte getIndex(String methodName) {
        if (methodName.length() == 1) {
            switch (methodName.charAt(0)) {
                case '+': return OP_PLUS;
                case '-': return OP_MINUS;
                case '<': return OP_LT;
                default: return NO_INDEX;
            }
        }
        if (methodName == "[]") return AREF;
        if (methodName == "[]=") return ASET;
        if (methodName == "pop") return POP;
        if (methodName == "push") return PUSH;
        if (methodName == "nil?") return NIL_P;
        if (methodName == "==") return EQUALEQUAL;
        if (methodName == "unshift") return UNSHIFT;
        if (methodName == ">=") return OP_GE;
        if (methodName == "<<") return OP_LSHIFT;
        if (methodName == "empty?") return EMPTY_P;
        return NO_INDEX;
    }
}
