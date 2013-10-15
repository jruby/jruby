package org.jruby.ir;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ir.persistence.IRPeristenceFacade;

/**
 * Abstract class that contains general logic for both IR Compiler and IR Interpreter
 *
 * @param <R> type of returned object by translator
 * @param <S> type of specific for translator object
 */
public abstract class IRTranslator<R, S> {
    public R performTranslation(Ruby runtime, Node node, S specificObject) {        
        IRScope producedIRScope = produceIrScope(runtime, node);
        
        if (isIRPersistenceRequired()) IRPeristenceFacade.persist(producedIRScope);
        
        return translationSpecificLogic(runtime, producedIRScope, specificObject);
    }
    
    protected abstract R translationSpecificLogic(Ruby runtime, IRScope producedIrScope, S specificObject);

    private static boolean isIRPersistenceRequired() {
        return RubyInstanceConfig.IR_PERSISTENCE;
    }

    private IRScope produceIrScope(Ruby runtime, Node node) {
        return IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildRoot((RootNode) node);
    }    

} 