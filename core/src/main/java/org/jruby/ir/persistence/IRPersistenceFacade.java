package org.jruby.ir.persistence;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.lexer.PersistedIRScanner;
import org.jruby.ir.persistence.parser.IRParsingContext;
import org.jruby.ir.persistence.parser.PersistedIRParser;
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

    public static void persist(IRScope irScopeToPersist, Ruby runtime)
            throws IRPersistenceException {
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
    
    public static void persist(IRScope irScopeToPersist, String fileName)
            throws IRPersistenceException {
        try {
            File irFile = new File(fileName);

            StringBuilder instructions = new StringBuilder();
            getIstructionsFromThisAndDescendantScopes(irScopeToPersist, instructions);
            FileIO.INSTANCE.writeToFile(irFile, instructions.toString());
        } catch (Exception e) {
            throw new IRPersistenceException(e);
        }

    }

    private static void getIstructionsFromThisAndDescendantScopes(IRScope irScopeToPersist,
            StringBuilder instructions) {
        instructions.append(irScopeToPersist.toPersistableString());
        instructions.append("\n\n");
        for (IRScope irScope : irScopeToPersist.getLexicalScopes()) {
            getIstructionsFromThisAndDescendantScopes(irScope, instructions);
        }
    }

    public static IRScope[] read(Ruby runtime) throws IRPersistenceException {
        IRParsingContext.INSTANCE.setRuntime(runtime);
        RubyInstanceConfig config = runtime.getInstanceConfig();
        File irFile = IRFileExpert.INSTANCE.getIRFileInIntendedPlace(config);
        try {
            String fileContent = FileIO.INSTANCE.readFile(irFile);
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(fileContent.getBytes(FileIO.CHARSET));
                PersistedIRScanner input = new PersistedIRScanner(is);
                return (IRScope[]) new PersistedIRParser().parse(input);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (FileNotFoundException e) {
            throw new IRPersistenceException(e);
        } catch (IOException e) {
            throw new IRPersistenceException(e);
        } catch (beaver.Parser.Exception e) {
            throw new IRPersistenceException(e);
        }
    }
}
