package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.lexer.ISourcePosition;

/**
 * f rescue nil
 */
public class RescueModParseNode extends RescueParseNode {
    public RescueModParseNode(ISourcePosition position, ParseNode bodyNode, RescueBodyParseNode rescueNode) {
        super(position, bodyNode, rescueNode, null /* else */);
    }
}
