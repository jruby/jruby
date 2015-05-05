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
public class ZYieldNode extends YieldNode {
    public ZYieldNode(ISourcePosition position) {
        super(position, null);
    }
}
