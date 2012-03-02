package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is of the form:
//    v = LBL_..
// Used in rescue blocks to tell the ensure block where to return to after it is done doing its thing.
public class SetReturnAddressInstr extends Instr implements ResultInstr {
    private Label returnAddr;
    private Variable result;

    public SetReturnAddressInstr(Variable result, Label l) {
        super(Operation.SET_RETADDR);
        
        assert result != null: "SetReturnAddressInstr result is null";
        
        this.returnAddr = l;
        this.result = result;
    }

    public Variable getResult() {
        return result;
    }
    
    public void updateResult(Variable v) {
        this.result = v;
    }

    public Label getReturnAddr() {
        return (Label) returnAddr;
    }

    public Operand[] getOperands() {
        return new Operand[]{returnAddr};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        /* Nothing to do since 'Label' is a not a variable */
    }

    @Override
    public String toString() {
        return "" + result + " = " + returnAddr;
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        return new SetReturnAddressInstr(ii.getRenamedVariable(result), ii.getRenamedLabel(returnAddr));
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new SetReturnAddressInstr(ii.getRenamedVariable(result), returnAddr);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return returnAddr;
    }
}
