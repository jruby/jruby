/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.lexer.yacc.ISourcePosition;

/**
 *
 * @author enebo
 */
public class WhenOneArgNode extends WhenNode {
    public WhenOneArgNode(ISourcePosition position, Node expressionNode, Node bodyNode, Node nextCase) {
        super(position, expressionNode, bodyNode, nextCase);
    }
}
