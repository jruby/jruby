/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * For SplatNode and ArgsCatNode calls.
 */
public class FCallSpecialArgBlockNode extends FCallNode {
    public FCallSpecialArgBlockNode(ISourcePosition position, String name, Node args, IterNode iter) {
        super(position, name, args, iter);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject arg = getArgsNode().interpret(runtime, context, self, aBlock);
        Block block = RuntimeHelpers.getBlock(context, self, iterNode);
        
        if (arg instanceof RubyArray) {
            RubyArray nodes = (RubyArray) arg;
            
            switch (nodes.size()) {
                case 0:
                    return callAdapter.callIter(context, self, self, block);
                case 1:
                    return callAdapter.callIter(context, self, self, nodes.eltInternal(0), block);
                case 2:
                    return callAdapter.callIter(context, self, self, nodes.eltInternal(0), nodes.eltInternal(1), block);
                case 3:
                    return callAdapter.callIter(context, self, self, nodes.eltInternal(0), nodes.eltInternal(1), nodes.eltInternal(2), block);
                default:
                    return callAdapter.callIter(context, self, self, nodes.toJavaArrayMaybeUnsafe(), block);
            }
        }
        
        return callAdapter.callIter(context, self, self, arg, block);
    }
}
