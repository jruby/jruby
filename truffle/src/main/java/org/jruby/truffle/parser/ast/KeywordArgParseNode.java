/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.List;

/**
 *
 * @author enebo
 */
public class KeywordArgParseNode extends ParseNode {
    private AssignableParseNode assignable;

    public KeywordArgParseNode(SourceIndexLength position, AssignableParseNode assignable) {
        super(position, true);
        this.assignable = assignable;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitKeywordArgNode(this);
    }

    @Override
    public List<ParseNode> childNodes() {
        return ParseNode.createList(assignable);
    }

    public int getIndex() {
        return ((IScopedNode) assignable).getIndex();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.KEYWORDARGNODE;
    }

    public AssignableParseNode getAssignable() {
        return assignable;
    }
    
}
