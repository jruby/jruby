/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast;

import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 * @author enebo
 */
public class Yield19Node extends YieldNode {
    public Yield19Node(ISourcePosition position, Node node) {
        super(ISourcePosition.INVALID_POSITION, node, false);
    }
}
