/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.List;

/**
 *
 * @author enebo
 */
public class ComplexParseNode extends NumericParseNode implements SideEffectFree {
    private NumericParseNode y;

    public ComplexParseNode(SourceIndexLength position, NumericParseNode y) {
        super(position);

        this.y = y;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
       return visitor.visitComplexNode(this);
    }

    @Override
    public List<ParseNode> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.COMPLEXNODE;
    }

    public NumericParseNode getNumber() {
        return y;
    }

    public void setNumber(NumericParseNode y) {
        this.y = y;
    }
}
