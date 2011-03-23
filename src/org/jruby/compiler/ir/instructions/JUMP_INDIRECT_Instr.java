package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// Used in ensure blocks to jump to the label contained in '_target'
public class JUMP_INDIRECT_Instr extends OneOperandInstr
{
    public JUMP_INDIRECT_Instr(Variable tgt) {
        super(Operation.JUMP_INDIRECT, null, tgt);
    }

    public Variable getJumpTarget() { return (Variable)getArg(); }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new JUMP_INDIRECT_Instr(ii.getRenamedVariable(getJumpTarget()));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        return (Label)(getArg().retrieve(interp));
    }
}
