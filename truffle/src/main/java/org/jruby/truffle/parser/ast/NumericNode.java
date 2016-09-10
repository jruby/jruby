package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.ast.types.ILiteralNode;
import org.jruby.truffle.parser.lexer.yacc.ISourcePosition;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericNode extends Node implements ILiteralNode {
    public NumericNode(ISourcePosition position) {
        super(position, false);
    }
}
