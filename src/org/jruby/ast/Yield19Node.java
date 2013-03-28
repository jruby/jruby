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
public class Yield19Node extends YieldNode {
    public Yield19Node(ISourcePosition position, Node node) {
        super(ISourcePosition.INVALID_POSITION, node, false);
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        Node args = getArgsNode();
        IRubyObject argsResult = args.interpret(runtime, context, self, aBlock);
        Block yieldToBlock = context.getCurrentFrame().getBlock();

        switch (args.getNodeType()) {
            case ARGSPUSHNODE:
            case ARGSCATNODE:
            case SPLATNODE: 
                argsResult = Helpers.unsplatValue19IfArityOne(argsResult, yieldToBlock);
                break;
            case ARRAYNODE:
                // Pass-thru
                break;
            default:
               assert false: "Invalid node found in yield";
        }

        return yieldToBlock.yieldArray(context, argsResult, null, null);
    }
}
