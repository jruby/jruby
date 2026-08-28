package org.jruby.ast;

import org.jruby.RubySymbol;
import org.jruby.ast.visitor.NodeVisitor;

public class KeywordRestArgNode extends ArgumentNode {
    public KeywordRestArgNode(int line, RubySymbol name, int index) {
        super(line, name, index);
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
