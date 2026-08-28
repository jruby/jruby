package org.jruby.ast;

import java.util.List;

import org.jruby.RubySymbol;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;

/**
 * A::B ||= 1
 */
public class OpAsgnConstDeclNode extends Node implements BinaryOperatorNode {
    private final Node lhs;
    private final RubySymbol operator;
    private final Node rhs;

    public OpAsgnConstDeclNode(int line, Node lhs, RubySymbol operator, Node rhs) {
        super(line, lhs.containsVariableAssignment() || rhs.containsVariableAssignment());

        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    public boolean isOr() {
        return CommonByteLists.OR_OR.equals(operator.getBytes());
    }

    public boolean isAnd() {
        return CommonByteLists.AMPERSAND_AMPERSAND.equals(operator.getBytes());
    }

    @Override
    // This can only be Colon3 or Colon2
    public Node getFirstNode() {
        return lhs;
    }

    @Override
    public Node getSecondNode() {
        return rhs;
    }

    public String getOperator() {
        return operator.asJavaString();
    }

    public ByteList getByteOperator() {
        return operator.getBytes();
    }

    public RubySymbol getSymbolOperator() {
        return operator;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitOpAsgnConstDeclNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return createList(lhs, new LiteralNode(getLine(), operator), rhs);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.OPASGNCONSTDECLNODE;
    }
}
