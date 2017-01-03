package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.List;

/**
 * Marker to indicate that rather than assigning nil (where in multiple
 * places we have nulls getting implicitly converted to nils) we should
 * raise an error.
 *
 * MRI passes a -1 as a special value so we are doing something similar
 * but more explicit.
 */
public class RequiredKeywordArgumentValueParseNode extends ParseNode implements InvisibleNode {
    public RequiredKeywordArgumentValueParseNode() {
        super(null, false);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitRequiredKeywordArgumentValueNode(this);
    }

    @Override
    public List<ParseNode> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.REQUIRED_KEYWORD_ARGUMENT_VALUE;
    }
}
