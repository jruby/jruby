package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.types.ILiteralNode;

/**
 * Any node representing a numeric value.
 */
public abstract class NumericParseNode extends ParseNode implements ILiteralNode {
    public NumericParseNode(SourceIndexLength position) {
        super(position, false);
    }
}
