package org.jruby.ir.persistence;

import java.io.File;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.util.FileIO;
import org.jruby.ir.persistence.util.IRFilePathManager;

public class IRPersistenceFacade {

    public static void persist(IRScope irScopeToPersist, Ruby runtime) throws IRPersistenceException {
        try {
            RubyInstanceConfig config = runtime.getInstanceConfig();
            
            // Place ir files inside ir folder under current directory
            File currentDir = new File(config.getCurrentDirectory());
            String rbFileName = config.displayedFileName();            
            File irFile = IRFilePathManager.INSTANCE.getIRFile(currentDir, rbFileName);                      

            String instructionsString = irScopeToPersist.toStringInstrs();
            FileIO.INSTANCE.writeToFile(irFile, instructionsString);
        } catch (Exception e) {
            throw new IRPersistenceException(e);
        }

    }
} 

