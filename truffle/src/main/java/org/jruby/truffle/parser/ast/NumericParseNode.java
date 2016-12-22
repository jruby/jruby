package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.TempSourceSection;
import org.jruby.truffle.parser.ast.types.ILiteralNode;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericParseNode extends ParseNode implements ILiteralNode {
    public NumericParseNode(TempSourceSection position) {
        super(position, false);
    }
}
