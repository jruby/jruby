/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * For SplatNode and ArgsCatNode calls.
 */
public class FCallSpecialArgNode extends FCallNode implements SpecialArgs {
    public FCallSpecialArgNode(ISourcePosition position, String name, Node args) {
        super(position, name, args, null);
    }

    @Override
    public Node setIterNode(Node iterNode) {
        return new FCallSpecialArgBlockNode(getPosition(), getName(), getArgsNode(), (IterNode) iterNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject arg = getArgsNode().interpret(runtime, context, self, aBlock);
        
        if (arg instanceof RubyArray) {
            RubyArray nodes = (RubyArray) arg;
            
            switch (nodes.size()) {
                case 0:
                    return callAdapter.call(context, self, self);
                case 1:
                    return callAdapter.call(context, self, self, nodes.eltInternal(0));
                case 2:
                    return callAdapter.call(context, self, self, nodes.eltInternal(0), nodes.eltInternal(1));
                case 3:
                    return callAdapter.call(context, self, self, nodes.eltInternal(0), nodes.eltInternal(1), nodes.eltInternal(2));
                default:
                    return callAdapter.call(context, self, self, nodes.toJavaArrayMaybeUnsafe());
            }
        }
        
        return callAdapter.call(context, self, self, arg);
    }
}
