/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyComplex;
import org.jruby.RubyObject;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class ComplexNode extends Node {
    private Node y;
    RubyComplex complex = null;
    
    public ComplexNode(ISourcePosition position, Node y) {
        super(position);

        this.y = y;
    }
    
    @Override
    public Object accept(NodeVisitor visitor) {
       return visitor.visitComplexNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.COMPLEXNODE;
    }
    
    public RubyComplex getComplex(Ruby runtime, IRubyObject y) {
        if (complex == null) {
            return complex = RubyComplex.newComplexRaw(runtime, runtime.newFixnum(0), y);
        }
        return complex;        
    }    

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return getComplex(runtime, y.interpret(runtime, context, self, aBlock));
    }
}
