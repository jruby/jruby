package org.jruby.ir;

import java.io.IOException;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.RootNode;
import org.jruby.ir.persistence.IRWriterFile;
import org.jruby.ir.persistence.IRWriter;
//import org.jruby.ir.persistence.persist.string.IRToStringTranslator;
//import org.jruby.ir.persistence.util.FileIO;
import org.jruby.ir.persistence.util.IRFileExpert;

/**
 * Abstract class that contains general logic for both IR Compiler and IR Interpreter
 *
 * @param <R> type of returned object by translator
 * @param <S> type of specific for translator object
 */
public abstract class IRTranslator<R, S> {
    public R execute(Ruby runtime, ParseResult result, S specificObject) {
        IRScope scope = null;

        if (result instanceof IRScope) { // Already have it (likely from read from persistent store).
            scope = (IRScope) result;
        } else if (result instanceof RootNode) { // Need to perform create IR from AST
            scope = IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildRoot((RootNode) result);

            if (RubyInstanceConfig.IR_WRITING) {
                try {
                    IRWriter.persist(new IRWriterFile(IRFileExpert.getIRPersistedFile(scope.getFileName())), scope);
//                    FileIO.writeToFile(IRFileExpert.getIRPersistedFile(scope.getFileName()),
//                            IRToStringTranslator.translate(scope));
                } catch (IOException ex) {
                    ex.printStackTrace(); // FIXME: Handle errors better
                    return null;
                }
            }
        }

        return execute(runtime, scope, specificObject);
    }

    protected abstract R execute(Ruby runtime, IRScope producedIrScope, S specificObject);
}
