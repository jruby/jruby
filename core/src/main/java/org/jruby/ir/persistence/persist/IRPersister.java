package org.jruby.ir.persistence.persist;

import java.io.File;
import java.io.IOException;
import org.jruby.ir.IRManager;

import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.IRPersistenceException;
import org.jruby.ir.persistence.persist.string.IRToStringTranslator;
import org.jruby.ir.persistence.util.FileIO;
import org.jruby.ir.persistence.util.IRFileExpert;


public class IRPersister {

    public static void persist(IRManager manager, IRScope irScopeToPersist) throws IRPersistenceException {
        try {
            String stringRepresentationOfIR = IRToStringTranslator.translate(irScopeToPersist);
            File irFile = IRFileExpert.getIRFileInIntendedPlace(manager.getFileName());
            
            FileIO.INSTANCE.writeToFile(irFile, stringRepresentationOfIR);
        } catch (IOException e) { // We do not want to brake current run, so catch even unchecked exceptions
            throw new IRPersistenceException(e);
        }

    }
}

