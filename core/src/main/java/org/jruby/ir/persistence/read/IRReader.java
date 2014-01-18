package org.jruby.ir.persistence.read;

import java.io.IOException;
import java.io.InputStream;

import org.jruby.Ruby;
import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.IRPersistenceException;
import org.jruby.ir.persistence.read.lexer.PersistedIRScanner;
import org.jruby.ir.persistence.read.parser.IRParsingContext;
import org.jruby.ir.persistence.read.parser.PersistedIRParser;
import org.jruby.parser.ParserSyntaxException;

public class IRReader {

    public static IRScope read(InputStream is, Ruby runtime) throws IRPersistenceException {
        try {
            PersistedIRScanner input = new PersistedIRScanner(is);
            PersistedIRParser parser = new PersistedIRParser(new IRParsingContext(runtime));
            return (IRScope) parser.yyparse(input);
        } catch (IOException e) {
            throw new IRPersistenceException(e);
        } catch (ParserSyntaxException ex) {
            throw new IRPersistenceException(ex);
        }
    }

}