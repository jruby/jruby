/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import java.util.List;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 * @author enebo
 */
public class RationalNode extends NumericNode implements SideEffectFree {
    private final long numerator;
    private final long denominator;

    public RationalNode(ISourcePosition position, long numerator, long denominator) {
        super(position);

        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitRationalNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.RATIONALNODE;
    }

    public long getNumerator() {
        return numerator;
    }

    public long getDenominator() {
        return denominator;
    }
}
