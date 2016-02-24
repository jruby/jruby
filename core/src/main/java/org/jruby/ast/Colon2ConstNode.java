/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jcodings.Encoding;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 * @author enebo
 */
public class Colon2ConstNode extends Colon2Node {
    public Colon2ConstNode(ISourcePosition position, Node leftNode, String name, Encoding encoding) {
        super(position, leftNode, name, encoding);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }
}
