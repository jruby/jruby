package org.jruby.ir.persistence.persist;

import java.io.File;

import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.IRPersistenceException;
import org.jruby.ir.persistence.persist.string.IRToStringTranslator;
import org.jruby.ir.persistence.read.IRReadingContext;
import org.jruby.ir.persistence.util.FileIO;
import org.jruby.ir.persistence.util.IRFileExpert;


public class IRPersister {

    public static void persist(IRScope irScopeToPersist)
            throws IRPersistenceException {
        try {
            String stringRepresentationOfIR = IRToStringTranslator.translate(irScopeToPersist);
            
            String rbFileName = IRReadingContext.INSTANCE.getFileName();
            File irFile = IRFileExpert.INSTANCE.getIRFileInIntendedPlace(rbFileName);
            FileIO.INSTANCE.writeToFile(irFile, stringRepresentationOfIR);
        } catch (Exception e) { // We do not want to brake current run, so catch even unchecked exceptions
            throw new IRPersistenceException(e);
        }

    }
}

