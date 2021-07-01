package org.jruby.ast;

import org.jruby.ast.types.ILiteralNode;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericNode extends Node implements ILiteralNode {
    public NumericNode(int line) {
        super(line, false);
    }

    public NumericNode negate() {
        throw new IllegalArgumentException("Unexpected negation of a numeric type");
    }
}
