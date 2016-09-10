package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.lexer.yacc.ISourcePosition;

/**
 * f rescue nil
 */
public class RescueModNode extends RescueNode {
    public RescueModNode(ISourcePosition position, Node bodyNode, RescueBodyNode rescueNode) {
        super(position, bodyNode, rescueNode, null /* else */);
    }
}
