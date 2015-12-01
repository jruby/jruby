package org.jruby.ast;

import org.jruby.lexer.yacc.ISourcePosition;

/**
 * f rescue nil
 */
public class RescueModNode extends RescueNode {
    public RescueModNode(ISourcePosition position, Node bodyNode, RescueBodyNode rescueNode) {
        super(position, bodyNode, rescueNode, null /* else */);
    }
}
