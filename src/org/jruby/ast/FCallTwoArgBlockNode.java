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
 *
 * @author enebo
 */
public class FCallTwoArgBlockNode extends FCallNode {
    private Node arg1;
    private Node arg2;
    
    public FCallTwoArgBlockNode(ISourcePosition position, String name, ArrayNode args, IterNode iter) {
        super(position, name, args, iter);
        
        assert args.size() == 2 : "args.size() is 2";
        
        arg1 = args.get(0);
        arg2 = args.get(1);
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return callAdapter.callIter(context, self, self,
                arg1.interpret(runtime, context, self, aBlock),
                arg2.interpret(runtime, context, self, aBlock),
                RuntimeHelpers.getBlock(context, self, iterNode));
    }
}
