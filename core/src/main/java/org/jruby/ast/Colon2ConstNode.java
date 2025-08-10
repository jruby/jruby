/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.RubySymbol;

public class Colon2ConstNode extends Colon2Node {
    public Colon2ConstNode(int line, Node leftNode, RubySymbol name) {
        super(line, leftNode, name);

        assert leftNode != null: "Colon2ConstNode cannot have null leftNode";
    }
}
