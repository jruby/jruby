package org.jruby.ir.persistence.parser.dummy;

import org.jruby.ir.Operation;

public class SingleParamInstr {
    
    private Operation operation;
    private Object parameter;
    
    public SingleParamInstr(Operation operation, Object parameter) {
        this.operation = operation;
        this.parameter = parameter;
    }
    
    public Operation getOperation() {
        return operation;
    }

    public Object getParameter() {
        return parameter;
    }    
} 