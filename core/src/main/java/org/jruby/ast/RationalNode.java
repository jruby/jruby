/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import java.util.List;
import org.jruby.ast.visitor.NodeVisitor;

/**
 *
 * @author enebo
 */
public class RationalNode extends NumericNode implements SideEffectFree {
    private final NumericNode numerator;
    private final NumericNode denominator;

    public RationalNode(int line, NumericNode numerator, NumericNode denominator) {
        super(line);

        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitRationalNode(this);
    }

    @Override
    public NumericNode negate() {
        return new RationalNode(getLine(), numerator.negate(), denominator);
    }

    @Override
    public List<Node> childNodes() {
        return Node.createList(numerator, denominator);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.RATIONALNODE;
    }

    public NumericNode getNumerator() {
        return numerator;
    }

    public NumericNode getDenominator() {
        return denominator;
    }
}
