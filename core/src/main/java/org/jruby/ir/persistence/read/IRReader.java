package org.jruby.ir.persistence.read;

import java.io.InputStream;

import org.jruby.Ruby;
import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.IRPersistenceException;
import org.jruby.ir.persistence.read.lexer.PersistedIRScanner;
import org.jruby.ir.persistence.read.parser.IRParsingContext;
import org.jruby.ir.persistence.read.parser.PersistedIRParser;

public class IRReader {

    public static IRScope read(InputStream is, Ruby runtime) throws IRPersistenceException {
        try {
            PersistedIRScanner input = new PersistedIRScanner(is);
            PersistedIRParser parser = new PersistedIRParser();
            IRParsingContext context = new IRParsingContext(runtime);
            parser.init(context);
            return (IRScope) parser.parse(input);
        } catch (Exception e) {
            throw new IRPersistenceException(e);
        }
    }

}