package org.jruby.ast;

import org.jruby.ast.types.ILiteralNode;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericNode extends Node implements ILiteralNode {
    public NumericNode(ISourcePosition position) {
        super(position, false);
    }
}
