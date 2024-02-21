package org.jruby.parser;

import org.jruby.Ruby;
import org.jruby.ir.builder.IRBuilderFactory;

public class ParserProviderDefault implements ParserProvider {
    public Parser getParser(Ruby runtime) {
        return new Parser(runtime);
    }

    public IRBuilderFactory getBuilderFactory() {
        return new IRBuilderFactory();
    }
}
