package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.runtime.ThreadContext;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstMissingInstr extends Instr implements ResultInstr {
    private Operand definingScope;
    private String  missingConst;
    private Variable result;

    public ConstMissingInstr(Variable result, Operand definingScope, String missingConst) {
        super(Operation.CONST_MISSING);
        
        assert result != null: "ConstMissingInstr result is null";
        
        this.definingScope = definingScope;
        this.missingConst = missingConst;
        this.result = result;
    }

    @Override
    public Operand[] getOperands() { 
        return new Operand[] { definingScope };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        definingScope = definingScope.getSimplifiedOperand(valueMap, force);
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }
    
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ConstMissingInstr(ii.getRenamedVariable(result), definingScope, missingConst);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + definingScope + "," + missingConst  + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        StaticScope staticScope = (StaticScope) definingScope.retrieve(context, self, currDynScope, temp);
        return staticScope.getModule().callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(missingConst));
    }
}
