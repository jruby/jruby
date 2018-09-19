package org.jruby.ast;

import org.jruby.RubySymbol;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 */
public class KeywordRestArgNode extends ArgumentNode {
    public KeywordRestArgNode(ISourcePosition position, RubySymbol name, int index) {
        super(position, name, index);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitKeywordRestArgNode(this);
    }
    
    @Override
    public NodeType getNodeType() {
        return NodeType.KEYWORDRESTARGNODE;
    }
}
