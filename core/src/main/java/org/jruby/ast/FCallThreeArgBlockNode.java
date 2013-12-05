/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class FCallThreeArgBlockNode extends FCallNode {
    private Node arg1;
    private Node arg2;
    private Node arg3;
    
    public FCallThreeArgBlockNode(ISourcePosition position, String name, ArrayNode args, IterNode iter) {
        super(position, name, args, iter);
        
        assert args.size() == 3 : "args.size() is 3";
        
        arg1 = args.get(0);
        arg2 = args.get(1);
        arg3 = args.get(2);
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return callAdapter.callIter(context, self, self,
                arg1.interpret(runtime, context, self, aBlock),
                arg2.interpret(runtime, context, self, aBlock),
                arg3.interpret(runtime, context, self, aBlock),
                Helpers.getBlock(context, self, iterNode));
    }
}
