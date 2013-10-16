package org.jruby.ir.persistence.parser.dummy;

import org.jruby.ir.Operation;
import org.jruby.ir.persistence.parser.NonIRObjectFactory;

public enum DummyInstrFactory {
    INSTANCE;
    
    public SingleParamInstr createSingleParamInstr(String name, Object param) {
        Operation operation = NonIRObjectFactory.INSTANCE.createOperation(name);
        return new SingleParamInstr(operation, param);
    }
    
    public MultipleParamInstr createMultipleParamInstr(String name, Object[] params) {
        Operation operation = NonIRObjectFactory.INSTANCE.createOperation(name);
        return new MultipleParamInstr(operation, params);
    }
} 