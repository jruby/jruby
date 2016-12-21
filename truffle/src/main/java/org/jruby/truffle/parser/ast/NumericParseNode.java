package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.ast.types.ILiteralNode;
import org.jruby.truffle.language.RubySourceSection;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericParseNode extends ParseNode implements ILiteralNode {
    public NumericParseNode(RubySourceSection position) {
        super(position, false);
    }
}
