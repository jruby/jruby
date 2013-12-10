package org.jruby.ir.persistence.persist.string;

import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.persist.string.producer.IRScopeStringBuilder;

class IRScopeStringExtractor {
    
    private final IRScopeStringBuilder stringProducer;
    
    private IRScopeStringExtractor(IRScopeStringBuilder stringProducer) {
        this.stringProducer = stringProducer;
    }
    
    // Static factory that is used in translator
    static IRScopeStringExtractor createToplevelInstance() {
        IRScopeStringBuilder stringProducer = new IRScopeStringBuilder(null);
        return new IRScopeStringExtractor(stringProducer);
    }

    String extract(IRScope irScope) {
        addScopeInfosRecursivelly(irScope);
        stringProducer.finishLine();
        addInstructionsRecursively(irScope);
        
        return stringProducer.getResultString();
    }

    private void addScopeInfosRecursivelly(IRScope irScope) {
        stringProducer.appendScopeInfo(irScope);
        
        for (IRScope innerScope : irScope.getLexicalScopes()) {
            addScopeInfosRecursivelly(innerScope);
        }
    }
    
    private void addInstructionsRecursively(IRScope irScope) {
        stringProducer.appendInstructions(irScope);
        stringProducer.finishLine();
        
        for (IRScope innerScope : irScope.getLexicalScopes()) {
            addInstructionsRecursively(innerScope);
        }
    }

}

