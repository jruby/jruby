package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.RubyMatchData;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class BackrefIsMatchDataInstr extends DefinedInstr {
    public BackrefIsMatchDataInstr(Variable result) {
        super(Operation.BACKREF_IS_MATCH_DATA, result, EMPTY_OPERANDS);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BackrefIsMatchDataInstr(ii.getRenamedVariable(result));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // SSS: FIXME: Or use this directly? "context.getCurrentScope().getBackRef(rt)" What is the diff??
        IRubyObject backref = RuntimeHelpers.getBackref(context.runtime, context);
        
        return context.runtime.newBoolean(RubyMatchData.class.isInstance(backref));        
    }
}
