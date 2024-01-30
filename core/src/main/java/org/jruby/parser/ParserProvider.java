package org.jruby.parser;

import org.jruby.Ruby;
import org.jruby.ir.builder.IRBuilderFactory;

/**
 * Provides parser and builder instances.
 */
public interface ParserProvider {
    Parser getParser(Ruby runtime);
    IRBuilderFactory getBuilderFactory();
}
