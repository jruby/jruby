package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.ast.types.ILiteralNode;
import org.jruby.truffle.parser.lexer.ISourcePosition;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericParseNode extends ParseNode implements ILiteralNode {
    public NumericParseNode(ISourcePosition position) {
        super(position, false);
    }
}
