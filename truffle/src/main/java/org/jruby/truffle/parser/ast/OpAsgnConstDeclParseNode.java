package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.ast.visitor.NodeVisitor;
import org.jruby.truffle.parser.lexer.ISourcePosition;

import java.util.List;

/**
 * A::B ||= 1
 */
public class OpAsgnConstDeclParseNode extends ParseNode implements BinaryOperatorParseNode {
    private ParseNode lhs;
    private String operator;
    private ParseNode rhs;

    public OpAsgnConstDeclParseNode(ISourcePosition position, ParseNode lhs, String operator, ParseNode rhs) {
        super(position, lhs.containsVariableAssignment() || rhs.containsVariableAssignment());

        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    @Override
    public ParseNode getFirstNode() {
        return lhs;
    }

    @Override
    public ParseNode getSecondNode() {
        return rhs;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitOpAsgnConstDeclNode(this);
    }

    @Override
    public List<ParseNode> childNodes() {
        return createList(lhs, new LiteralParseNode(getPosition(), operator), rhs);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.OPASGNCONSTDECLNODE;
    }
}
