package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstMissingInstr extends Instr implements ResultInstr {
    private Operand currentModule;
    private String  missingConst;
    private Variable result;

    public ConstMissingInstr(Variable result, Operand currentModule, String missingConst) {
        super(Operation.CONST_MISSING);
        
        assert result != null: "ConstMissingInstr result is null";
        
        this.currentModule = currentModule;
        this.missingConst = missingConst;
        this.result = result;
    }

    @Override
    public Operand[] getOperands() { 
        return new Operand[] { currentModule };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        currentModule = currentModule.getSimplifiedOperand(valueMap, force);
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }
    
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ConstMissingInstr(ii.getRenamedVariable(result), currentModule, missingConst);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + currentModule + "," + missingConst  + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        RubyModule module = (RubyModule) currentModule.retrieve(context, self, currDynScope, temp);
        return module.callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(missingConst));
    }
}
