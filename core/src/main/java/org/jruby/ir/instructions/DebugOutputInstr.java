package org.jruby.ir.instructions;

import org.jruby.ir.operands.Operand;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This is only used for active debugging and should never end up showing up in real executed code.
 * If it did happen to sneak in and it was persisted it would convert to a noop instr.
 */
public class DebugOutputInstr extends NopInstr {
    private String message;
    private Operand[] operands;

    public DebugOutputInstr(String message, Operand[] operands) {
        super();

        this.message = message;
        this.operands = operands;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object[] values = new Object[operands.length];

        for (int i = 0; i < values.length; i++) {
            values[i] = operands[i].retrieve(context, self, currScope, currDynScope, temp);
        }

        System.out.println(String.format(message, values));
        return context.nil;
    }
}
