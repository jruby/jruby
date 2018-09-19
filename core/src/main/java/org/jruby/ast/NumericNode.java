package org.jruby.ast;

import org.jruby.ast.types.ILiteralNode;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SyntaxException;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericNode extends Node implements ILiteralNode {
    public NumericNode(ISourcePosition position) {
        super(position, false);
    }

    public NumericNode negate() {
        throw new IllegalArgumentException("Unexpected negation of a numeric type");
    }
}
