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
public class YieldTwoNode extends YieldNode {
    private final Node argument1;
    private final Node argument2;

    public YieldTwoNode(ISourcePosition position, ArrayNode args) {
        super(position, args, true);

        argument1 = args.get(0);
        argument2 = args.get(1);
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return context.getCurrentFrame().getBlock().yieldSpecific(context,
                argument1.interpret(runtime, context, self, aBlock),
                argument2.interpret(runtime, context, self, aBlock));
    }
}
