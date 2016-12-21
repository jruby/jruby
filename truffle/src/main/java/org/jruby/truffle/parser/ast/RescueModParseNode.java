package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.lexer.SimpleSourcePosition;

/**
 * f rescue nil
 */
public class RescueModParseNode extends RescueParseNode {
    public RescueModParseNode(SimpleSourcePosition position, ParseNode bodyNode, RescueBodyParseNode rescueNode) {
        super(position, bodyNode, rescueNode, null /* else */);
    }
}
