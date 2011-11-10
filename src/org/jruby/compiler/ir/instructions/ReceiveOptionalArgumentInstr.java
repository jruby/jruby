package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Assign the 'index' argument to 'dest'.
public class ReceiveOptionalArgumentInstr extends Instr implements ResultInstr {
    int argIndex;
    private final Variable result;
    
    public ReceiveOptionalArgumentInstr(Variable result, int index) {
        super(Operation.RECV_OPT_ARG);
        
        assert result != null: "ReceiveOptionalArgumentInstr result is null";
        
        this.argIndex = index;
        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }
    
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveOptionalArgumentInstr(ii.getRenamedVariable(result), argIndex);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, IRExecutionScope scope, ThreadContext context, IRubyObject self, org.jruby.runtime.Block block) {
        Object v = interp.getParameterCount() > argIndex ? 
                interp.getParameter(argIndex) : UndefinedValue.UNDEFINED;
        result.store(interp, context, self, v);
        return null;
    }
}
