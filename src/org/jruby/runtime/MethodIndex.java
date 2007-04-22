/*
 * MethodIndex.java
 *
 * Created on January 1, 2007, 7:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author headius
 */
public class MethodIndex {
    public static final int NO_INDEX = 0;
    public static final int OP_PLUS = 1;
    public static final int OP_MINUS = 2;
    public static final int OP_LT = 3;
    public static final int AREF = 4;
    public static final int ASET = 5;
    public static final int POP = 6;
    public static final int PUSH = 7;
    public static final int NIL_P = 8;
    public static final int EQUALEQUAL = 9;
    public static final int UNSHIFT = 10;
    public static final int OP_GE = 11;
    public static final int OP_LSHIFT = 12;
    public static final int EMPTY_P = 13;
    public static final int TO_S = 14;
    public static final int TO_I = 15;
    public static final int AT = 16;
    public static final int TO_STR = 17;
    public static final int TO_ARY = 18;
    public static final int TO_INT = 19;
    public static final int TO_F = 20;
    public static final int TO_SYM = 21;
    public static final int TO_A = 22;
    public static final int HASH = 23;
    public static final int OP_GT = 24;
    public static final int OP_TIMES = 25;
    public static final int OP_LE = 26;
    public static final int OP_SPACESHIP = 27;
    public static final int LENGTH = 28;
    public static final int OP_MATCH = 29;
    public static final int OP_EQQ = 30;
    public static final int LAST = 31;
    public static final int SHIFT = 32;
    public static final int EQL_P = 33;
    public static final int TO_HASH = 34;
    public static final int MAX_METHODS = 35;
    
    public static final String[] NAMES = new String[MAX_METHODS];
    public static final Map NUMBERS = new HashMap();
    
    static {
        NAMES[NO_INDEX] = "";
        NAMES[OP_PLUS] = "+";
        NAMES[OP_MINUS] = "-";
        NAMES[OP_LT] = "<";
        NAMES[AREF] = "[]";
        NAMES[ASET] = "[]=";
        NAMES[POP] = "pop";
        NAMES[PUSH] = "push";
        NAMES[NIL_P] = "nil?";
        NAMES[EQUALEQUAL] = "==";
        NAMES[UNSHIFT] = "unshift";
        NAMES[OP_GE] = ">=";
        NAMES[OP_LSHIFT] = "<<";
        NAMES[EMPTY_P] = "empty?";
        NAMES[TO_S] = "to_s";
        NAMES[TO_I] = "to_i";
        NAMES[AT] = "at";
        NAMES[TO_STR] = "to_str";
        NAMES[TO_ARY] = "to_ary";
        NAMES[TO_INT] = "to_int";
        NAMES[TO_F] = "to_f";
        NAMES[TO_SYM] = "to_sym";
        NAMES[TO_A] = "to_a";
        NAMES[HASH] = "hash";
        NAMES[OP_GT] = ">";
        NAMES[OP_TIMES] = "*";
        NAMES[OP_LE] = "<=";
        NAMES[OP_SPACESHIP] = "<=>";
        NAMES[LENGTH] = "length";
        NAMES[OP_MATCH] = "=~";
        NAMES[OP_EQQ] = "===";
        NAMES[LAST] = "last";
        NAMES[SHIFT] = "shift";
        NAMES[EQL_P] = "eql?";
        NAMES[TO_HASH] = "to_hash";
        
        for (int i = 0; i < MAX_METHODS; i++) {
            NUMBERS.put(NAMES[i], new Integer(i));
        }
    }
    
    /** Creates a new instance of MethodIndex */
    public MethodIndex() {
    }
    
    public static int getIndex(String methodName) {
        // fast lookup for the length 1 messages
        switch (methodName.length()) {
        case 1:
            switch (methodName.charAt(0)) {
            case '+': return OP_PLUS;
            case '-': return OP_MINUS;
            case '<': return OP_LT;
            case '>': return OP_GT;
            case '*': return OP_TIMES;
            default: return NO_INDEX;
            }
        default:
            if (NUMBERS.containsKey(methodName)) return ((Integer)NUMBERS.get(methodName)).intValue();
            return NO_INDEX;
        }
    }
}
