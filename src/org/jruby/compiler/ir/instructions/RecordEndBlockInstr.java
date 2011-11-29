package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RecordEndBlockInstr extends Instr {
	 private IRExecutionScope declaringScope;
	 private IRClosure endBlockClosure;

    public RecordEndBlockInstr(IRExecutionScope declaringScope, IRClosure endBlockClosure) {
        super(Operation.RECORD_END_BLOCK);
        
        this.declaringScope = declaringScope;
        this.endBlockClosure = endBlockClosure;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RecordEndBlockInstr(declaringScope, endBlockClosure);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        declaringScope.getTopLevelScope().recordEndBlock(endBlockClosure);
        return null;
    }
}
