package org.jruby.ir.persistence.parser.dummy;

import java.util.List;

import org.jruby.ir.Operation;

public class MultipleParamInstr {
    private Operation operation;
    private List<Object> parameters;
    
    public MultipleParamInstr(Operation operation, List<Object> parameters) {
        this.operation = operation;
        this.parameters = parameters;        
    }
    
    public Operation getOperation() {
        return operation;
    }

    public List<Object> getParameters() {
        return parameters;
    }
}
