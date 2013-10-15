package org.jruby.ir.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.persistence.util.FileIO;
import org.jruby.ir.persistence.util.IRFileExpert;

// This class currently contains code that will be decoupled later on 
public class IRPersistenceFacade {
    
    public static boolean isPersistedIrExecution(Ruby runtime) {
        RubyInstanceConfig config = runtime.getInstanceConfig();
        String fileName = config.displayedFileName();
        boolean isIrFileName = IRFileExpert.INSTANCE.isIrFileName(fileName);
        return isIrFileName || IRFileExpert.INSTANCE.isIrFileForRbFileFound(config);
    }
 
    public static void persist(IRScope irScopeToPersist, Ruby runtime) throws IRPersistenceException {
         try {
             RubyInstanceConfig config = runtime.getInstanceConfig();
             File irFile = IRFileExpert.INSTANCE.getIRFileInIntendedPlace(config);

             StringBuilder instructions = new StringBuilder();
             getIstructionsFromThisAndDescendantScopes(irScopeToPersist, instructions);
             FileIO.INSTANCE.writeToFile(irFile, instructions.toString());

        } catch (Exception e) {
            throw new IRPersistenceException(e);
        }

    }

    private static void getIstructionsFromThisAndDescendantScopes(IRScope irScopeToPersist,
            StringBuilder instructions) {
        instructions.append(irScopeToPersist.toStringInstrs());
        for(IRScope irScope : irScopeToPersist.getLexicalScopes()) {
            instructions.append("\n");
            getIstructionsFromThisAndDescendantScopes(irScope, instructions);            
        }
    }

    public static IRScope read(Ruby runtime) throws IRPersistenceException {
        try {
            RubyInstanceConfig config = runtime.getInstanceConfig();
            File irFile = IRFileExpert.INSTANCE.getIRFileInIntendedPlace(config);
            
            // FIXME: It is copy/paste from IRBuilder#buildRoot, 
            // maybe we will need to read IR that was not produced by IRBuilder#buildRoot? 
            IRScriptBody script = new IRScriptBody(runtime.getIRManager(), "__file__", irFile.getAbsolutePath(), null);
            script.addInstr(new ReceiveSelfInstr(script.getSelf()));
            // Set %current_scope = <current-scope>
            // Set %current_module = <current-module>
            script.addInstr(new CopyInstr(script.getCurrentScopeVariable(), new CurrentScope(script)));
            script.addInstr(new CopyInstr(script.getCurrentModuleVariable(), new ScopeModule(script)));
            // end of copy/paste
            
            // read line by line and transform every line with instruction into instruction
            String[] fileLines = FileIO.INSTANCE.readFile(irFile);
            for (String line : fileLines) {
                
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

} 

