package org.jruby.parser;

import org.jruby.Ruby;
import org.jruby.ir.builder.IRBuilderFactory;

/**
 * Provides parser and builder instances.
 */
public interface ParserProvider {
    /**
     * Initialize if backend of provider has not already been initialized (native bindings might be static
     * and initializable only once).
     * @param path path to where provider should provide a parser for
     */
    default void initialize(String path) {}
    Parser getParser(Ruby runtime);
    IRBuilderFactory getBuilderFactory();
}
