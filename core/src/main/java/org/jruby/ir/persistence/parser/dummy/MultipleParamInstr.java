package org.jruby.ir.persistence.parser.dummy;

import org.jruby.ir.Operation;

public class MultipleParamInstr {
    private Operation operation;
    private Object[] parameters;
    
    public MultipleParamInstr(Operation operation, Object[] parameters) {
        this.operation = operation;
        this.parameters = parameters;        
    }
    
    public Operation getOperation() {
        return operation;
    }

    public Object[] getParameters() {
        return parameters;
    }
} 