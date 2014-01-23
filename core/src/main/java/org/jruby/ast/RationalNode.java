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
    RubyRational rational;
    
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
    
    public RubyRational getRational(Ruby runtime) {
        if (rational == null) {
            return rational = runtime.newRational(numerator, 1);
        }
        return rational;        
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return getRational(runtime);
    } 
}
