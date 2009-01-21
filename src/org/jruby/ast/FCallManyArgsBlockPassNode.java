/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * For SplatNode and ArgsCatNode calls.
 */
public class FCallManyArgsBlockPassNode extends FCallNode {
    public FCallManyArgsBlockPassNode(ISourcePosition position, String name, Node args, BlockPassNode iter) {
        super(position, name, args, iter);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject[] args = ((ArrayNode) getArgsNode()).interpretPrimitive(runtime, context, self, aBlock);
        
        return callAdapter.call(context, self, self, args, RuntimeHelpers.getBlock(runtime, context, self, iterNode, aBlock));
    }
}
