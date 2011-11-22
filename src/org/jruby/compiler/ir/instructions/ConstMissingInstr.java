package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRModule;
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
    private IRModule definingModule;
    private String  missingConst;
    private Variable result;

    public ConstMissingInstr(Variable result, IRModule definingModule, String missingConst) {
        super(Operation.CONST_MISSING);
        
        assert result != null: "ConstMissingInstr result is null";
        
        this.definingModule = definingModule;
        this.missingConst = missingConst;
        this.result = result;
    }

    public Operand[] getOperands() { 
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }
    
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ConstMissingInstr(ii.getRenamedVariable(result), definingModule, missingConst);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + (definingModule == null ? "-dynamic-" : definingModule.getName()) + "," + missingConst  + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        StaticScope staticScope = definingModule == null ? context.getCurrentScope().getStaticScope() : definingModule.getStaticScope();
        return staticScope.getModule().callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(missingConst));
    }
}
