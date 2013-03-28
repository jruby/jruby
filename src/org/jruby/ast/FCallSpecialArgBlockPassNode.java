/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class FCallSpecialArgBlockPassNode extends FCallNode implements SpecialArgs {
    // For 'foo()'.  Args are only significant in maintaining backwards compatible AST structure
    public FCallSpecialArgBlockPassNode(ISourcePosition position, String name, Node args, BlockPassNode iter) {
        super(position, name, args, iter);
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject arg = getArgsNode().interpret(runtime, context, self, aBlock);
        Block block = Helpers.getBlock(runtime, context, self, iterNode, aBlock);
        
        if (arg instanceof RubyArray) {
            RubyArray nodes = (RubyArray) arg;
            
            switch (nodes.size()) {
                case 0:
                    return callAdapter.call(context, self, self, block);
                case 1:
                    return callAdapter.call(context, self, self, nodes.eltInternal(0), block);
                case 2:
                    return callAdapter.call(context, self, self, nodes.eltInternal(0), nodes.eltInternal(1), block);
                case 3:
                    return callAdapter.call(context, self, self, nodes.eltInternal(0), nodes.eltInternal(1), nodes.eltInternal(2), block);
                default:
                    return callAdapter.call(context, self, self, nodes.toJavaArrayMaybeUnsafe(), block);
            }
        }
        
        return callAdapter.call(context, self, self, arg, block);
    }
}
