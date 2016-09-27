/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.truffle.parser.parser;

/**
 *
 * @author enebo
 */
public class ParserSyntaxException extends Exception {
    public ParserSyntaxException(String message, String... args) {
        super(message);
    }
}
