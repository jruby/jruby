package org.jruby.ir;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public R performTranslation(Ruby runtime, ParseResult reset, S specificObject) {
        IRScope scope = null;
        
        if (reset instanceof IRScope) { // Already have it (likely from read from persistent store).
            scope = (IRScope) reset;
        } else if (reset instanceof RootNode) { // Need to perform create IR from AST
            scope = IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildRoot((RootNode) reset);

            if (RubyInstanceConfig.IR_PERSISTENCE) {
                try {
                    persist(scope);
                } catch (IRPersistenceException ex) {
                    ex.printStackTrace(); // FIXME: Handle errors better
                    return null;
                }
            }
        }

        // Execute the IR.
        return translationSpecificLogic(runtime, scope, specificObject);                
    }

    protected abstract R translationSpecificLogic(Ruby runtime, IRScope producedIrScope, S specificObject);

    
    private void persist(IRScope scope) throws IRPersistenceException {
        try {
            File irFile = IRFileExpert.getIRFileInIntendedPlace(scope.getFileName());
            
            FileIO.writeToFile(irFile, IRToStringTranslator.translate(scope));
        } catch (IOException e) { // We do not want to brake current run, so catch even unchecked exceptions
            throw new IRPersistenceException(e);
        }
    }    
}
