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
    public static final int MAX_METHODS = 26;
    
    public static final String[] NAMES = new String[MAX_METHODS];
    public static final Map NUMBERS = new HashMap();
    
    static {
        NAMES[0] = "";
        NAMES[1] = "+";
        NAMES[2] = "-";
        NAMES[3] = "<";
        NAMES[4] = "[]";
        NAMES[5] = "[]=";
        NAMES[6] = "pop";
        NAMES[7] = "push";
        NAMES[8] = "nil?";
        NAMES[9] = "==";
        NAMES[10] = "unshift";
        NAMES[11] = ">=";
        NAMES[12] = "<<";
        NAMES[13] = "empty?";
        NAMES[14] = "to_s";
        NAMES[15] = "to_i";
        NAMES[16] = "at";
        NAMES[17] = "to_str";
        NAMES[18] = "to_ary";
        NAMES[19] = "to_int";
        NAMES[20] = "to_f";
        NAMES[21] = "to_sym";
        NAMES[22] = "to_a";
        NAMES[23] = "hash";
        NAMES[24] = ">";
        NAMES[25] = "*";
        
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
