package org.jruby.compiler.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class MatchInstr extends Instr implements ResultInstr {
    private final Variable result;
    private final Operand receiver;
    
    public MatchInstr(Variable result, Operand receiver) {
        super(Operation.MATCH);
        
        assert result != null: "MatchInstr result is null";
        
        this.result = result;
        this.receiver = receiver;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { receiver };
    }

    public Variable getResult() {
        return result;
    }
    
    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new MatchInstr((Variable) result.cloneForInlining(ii), receiver.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRExecutionScope scope, ThreadContext context, IRubyObject self, org.jruby.runtime.Block block) {
        RubyRegexp regexp = (RubyRegexp) receiver.retrieve(interp, context, self);
        result.store(interp, context, self, regexp.op_match2(context));
        return null;
    }
}
