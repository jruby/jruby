package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.runtime.ThreadContext;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstMissingInstr extends Instr implements ResultInstr {
    private IRModule definingModule;
    private String  missingConst;
    private final Variable result;

    public ConstMissingInstr(Variable result, IRModule definingModule, String missingConst) {
        super(Operation.CONST_MISSING);
        this.definingModule = definingModule;
        this.missingConst = missingConst;
        this.result = result;
    }

    public Operand[] getOperands() { 
        return new Operand[] {};
    }
    
    public Variable getResult() {
        return result;
    }
    
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ConstMissingInstr(ii.getRenamedVariable(result), definingModule, missingConst);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + (definingModule == null ? "-dynamic-" : definingModule.getName()) + "," + missingConst  + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        StaticScope staticScope = definingModule == null ? context.getCurrentScope().getStaticScope() : definingModule.getStaticScope();
        Object constant = staticScope.getModule().callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(missingConst));
        result.store(interp, context, self, constant);
        
        return null;
    }
}
