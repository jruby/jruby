/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class YieldOneNode extends YieldNode {
    private final Node argument1;

    public YieldOneNode(ISourcePosition position, ArrayNode args) {
        super(position, args, true);

        argument1 = args.get(0);
    }

    public YieldOneNode(ISourcePosition position, FixnumNode arg) {
        super(position, arg, true);

        argument1 = arg;
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return context.getCurrentFrame().getBlock().yieldSpecific(context,
                argument1.interpret(runtime, context, self, aBlock));
    }
}
