/*
 * MethodIndex.java
 *
 * Created on January 1, 2007, 7:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author headius
 */
public class MethodIndex {
    public static final List NAMES = new ArrayList();
    private static final Map NUMBERS = new HashMap();
    private static final Map CALL_ADAPTERS = new HashMap();
    private static final Map FUNCTION_ADAPTERS = new HashMap();
    private static final Map VARIABLE_ADAPTERS = new HashMap();
    
    // ensure zero is devoted to no method name
    public static final int NO_INDEX = getIndex("");
    
    // predefine a few other methods we invoke directly elsewhere
    public static final int OP_PLUS = getIndex("+");
    public static final int OP_MINUS = getIndex("-");
    public static final int OP_LT = getIndex("<");
    public static final int AREF = getIndex("[]");
    public static final int ASET = getIndex("[]=");
    public static final int EQUALEQUAL = getIndex("==");
    public static final int OP_LSHIFT = getIndex("<<");
    public static final int EMPTY_P = getIndex("empty?");
    public static final int TO_S = getIndex("to_s");
    public static final int TO_I = getIndex("to_i");
    public static final int TO_STR = getIndex("to_str");
    public static final int TO_ARY = getIndex("to_ary");
    public static final int TO_INT = getIndex("to_int");
    public static final int TO_F = getIndex("to_f");
    public static final int TO_A = getIndex("to_a");
    public static final int HASH = getIndex("hash");
    public static final int OP_GT = getIndex(">");
    public static final int OP_TIMES = getIndex("*");
    public static final int OP_LE = getIndex("<=");
    public static final int OP_SPACESHIP = getIndex("<=>");
    public static final int OP_EQQ = getIndex("===");
    public static final int EQL_P = getIndex("eql?");
    public static final int TO_HASH = getIndex("to_hash");
    public static final int METHOD_MISSING = getIndex("method_missing");
    public static final int DEFAULT = getIndex("default");

    /** Creates a new instance of MethodIndex */
    public MethodIndex() {
    }
    
    public synchronized static int getIndex(String methodName) {
        Integer index = (Integer)NUMBERS.get(methodName);
        
        if (index == null) {
            index = new Integer(NAMES.size());
            NUMBERS.put(methodName, index);
            NAMES.add(methodName);
        }
        
        return index.intValue();
    }
    
    public synchronized static CallAdapter getCallAdapter(String name) {
        int index = getIndex(name);
        CallAdapter callAdapter = (CallAdapter)CALL_ADAPTERS.get(new Integer(index));
        
        if (callAdapter == null) {
            callAdapter = new CallAdapter.DefaultCallAdapter(index, name, CallType.NORMAL);
            CALL_ADAPTERS.put(new Integer(index), callAdapter);
        }
        
        return callAdapter;
    }
    
    public synchronized static CallAdapter getFunctionAdapter(String name) {
        int index = getIndex(name);
        CallAdapter callAdapter = (CallAdapter)FUNCTION_ADAPTERS.get(new Integer(index));
        
        if (callAdapter == null) {
            callAdapter = new CallAdapter.DefaultCallAdapter(index, name, CallType.FUNCTIONAL);
            FUNCTION_ADAPTERS.put(new Integer(index), callAdapter);
        }
        
        return callAdapter;
    }
    
    public synchronized static CallAdapter getVariableAdapter(String name) {
        int index = getIndex(name);
        CallAdapter callAdapter = (CallAdapter)VARIABLE_ADAPTERS.get(new Integer(index));
        
        if (callAdapter == null) {
            callAdapter = new CallAdapter.DefaultCallAdapter(index, name, CallType.VARIABLE);
            VARIABLE_ADAPTERS.put(new Integer(index), callAdapter);
        }
        
        return callAdapter;
    }
}
