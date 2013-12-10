package org.jruby.ir.persistence.read.parser.dummy;

import java.util.List;

import org.jruby.ir.Operation;
import org.jruby.ir.persistence.read.parser.NonIRObjectFactory;

public enum DummyInstrFactory {
    INSTANCE;
    
    public InstrWithParams createInstrWithParam(String name, List<Object> params) {
        Operation operation = NonIRObjectFactory.INSTANCE.createOperation(name);
        return new InstrWithParams(operation, params);
    }
}

