/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyRational;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class RationalNode extends Node {
    long numerator;

    public RationalNode(ISourcePosition position, long numerator) {
        super(position);
        
        this.numerator = numerator;
    }

    @Override
    public Object accept(NodeVisitor visitor) {
        return visitor.visitRationalNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.RATIONALNODE;
    }
}
