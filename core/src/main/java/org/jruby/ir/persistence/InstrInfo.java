package org.jruby.ir.persistence;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

public class InstrInfo {
    Variable result;
    Operation operation;
    Operand[] operands;
    
    InstrInfo(String lvalue, String name, String paramsString) {
        this(name, paramsString);
        // TODO: deal with lvalue
    }

    InstrInfo(String name, String paramsString) {
        // TODO: deal with name and paramsString
    }
} 
