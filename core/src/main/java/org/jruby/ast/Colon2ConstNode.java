/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.util.ByteList;

/**
 *
 * @author enebo
 */
public class Colon2ConstNode extends Colon2Node {
    public Colon2ConstNode(ISourcePosition position, Node leftNode, ByteList name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }

    @Deprecated
    public Colon2ConstNode(ISourcePosition position, Node leftNode, String name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }
}
