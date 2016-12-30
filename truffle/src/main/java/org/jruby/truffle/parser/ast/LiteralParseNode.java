package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.List;

/**
 * This is not a node in the classic sense in that it has no defined or
 * interpret method which can be called.  It just stores the position of
 * the literal and the name/value of the literal.  We made it a node so that
 * the parser needs to work less hard in its productions.  dynamic literals
 * are nodes and by having literals also be nodes means they have a common
 * subtype which is not Object.
 */
public class LiteralParseNode extends ParseNode implements InvisibleNode {
    private String name;

    public LiteralParseNode(SourceIndexLength position, String name) {
        super(position, false);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitLiteralNode(this);
    }

    public List<ParseNode> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.LITERALNODE;
    }

}
