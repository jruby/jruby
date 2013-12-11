package org.jruby.ir.persistence.read.parser.dummy;

import java.util.List;

import org.jruby.ir.persistence.read.parser.NonIRObjectFactory;

public class DummyInstrFactory {
    public static InstrWithParams createInstrWithParam(String name, List<Object> params) {
        return new InstrWithParams(NonIRObjectFactory.createOperation(name), params);
    }
}

