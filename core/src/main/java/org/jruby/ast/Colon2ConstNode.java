/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.RubySymbol;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 * @author enebo
 */
public class Colon2ConstNode extends Colon2Node {
    public Colon2ConstNode(ISourcePosition position, Node leftNode, RubySymbol name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }
}
