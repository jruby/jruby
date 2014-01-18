package org.jruby.ir.persistence.read.parser.dummy;

import java.util.List;

import org.jruby.ir.Operation;

public class InstrWithParams {
    private Operation operation;
    private List<Object> parameters;

    public InstrWithParams(Operation operation, List<Object> parameters) {
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
