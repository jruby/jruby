package org.jruby.ir;

import java.io.File;
import java.io.IOException;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.RootNode;
import org.jruby.ir.persistence.IRPersistenceException;
import org.jruby.ir.persistence.persist.string.IRToStringTranslator;
import org.jruby.ir.persistence.util.FileIO;
import org.jruby.ir.persistence.util.IRFileExpert;

/**
 * Abstract class that contains general logic for both IR Compiler and IR
 * Interpreter
 * 
 * @param <R>
 *            type of returned object by translator
 * @param <S>
 *            type of specific for translator object
 */
public abstract class IRTranslator<R, S> {

    public R performTranslation(Ruby runtime, ParseResult parseResult, S specificObject) {
        R result = null;
        try {

            IRScope producedIRScope = null;
            if (parseResult instanceof RootNode) {                
                
                RootNode rootNode = (RootNode) parseResult;
                if (isIRPersistenceRequired()) {
                    producedIRScope = produceIrScope(runtime, rootNode, false);
                    persist(runtime.getIRManager(), producedIRScope);
                    result = translationSpecificLogic(runtime, producedIRScope, specificObject);
                } else {
                    producedIRScope = produceIrScope(runtime, rootNode, false);
                    result = translationSpecificLogic(runtime, producedIRScope, specificObject);
                }                
                
            } else if (parseResult instanceof IRScope){
                producedIRScope = (IRScope) parseResult;
                result = translationSpecificLogic(runtime, producedIRScope, specificObject);
                
            } else {
                throw new IllegalArgumentException();
                
            }

        } catch (IRPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    protected abstract R translationSpecificLogic(Ruby runtime, IRScope producedIrScope,
            S specificObject);

    private static boolean isIRPersistenceRequired() {
        return RubyInstanceConfig.IR_PERSISTENCE;
    }

    private IRScope produceIrScope(Ruby runtime, RootNode rootNode, boolean isDryRun) {
        IRManager irManager = runtime.getIRManager();
        irManager.setDryRun(isDryRun);
        IRBuilder irBuilder = IRBuilder.createIRBuilder(runtime, irManager);

        final IRScope irScope = irBuilder.buildRoot(rootNode);
        return irScope;
    }
    
    private void persist(IRManager manager, IRScope irScopeToPersist) throws IRPersistenceException {
        try {
            String stringRepresentationOfIR = IRToStringTranslator.translate(irScopeToPersist);
            File irFile = IRFileExpert.getIRFileInIntendedPlace(manager.getFileName());
            
            FileIO.INSTANCE.writeToFile(irFile, stringRepresentationOfIR);
        } catch (IOException e) { // We do not want to brake current run, so catch even unchecked exceptions
            throw new IRPersistenceException(e);
        }
    }    
}
