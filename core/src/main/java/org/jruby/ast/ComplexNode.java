/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

public class ComplexNode extends NumericNode implements SideEffectFree {
    private NumericNode y;

    public ComplexNode(int line, NumericNode y) {
        super(line);

        this.y = y;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
       return visitor.visitComplexNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return Node.createList(y);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.COMPLEXNODE;
    }

    public NumericNode getNumber() {
        return y;
    }

    public void setNumber(NumericNode y) {
        this.y = y;
    }

    @Override
    public IRubyObject literalValue(Ruby runtime) {
        return IRRuntimeHelpers.newComplexRaw(runtime.getCurrentContext(), y.literalValue(runtime));
    }
}
