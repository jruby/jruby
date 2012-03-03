package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;

public class CheckArityInstr extends Instr {
    public final int required;
    public final int opt;
    public final int rest;
    
    public CheckArityInstr(int required, int opt, int rest) {
        super(Operation.CHECK_ARITY);
        
        this.required = required;
        this.opt = opt;
        this.rest = rest;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + required + ", " + opt + ", " + rest + ")";
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        if (ii.canMapArgsStatically()) {
            // Since we know arity at a callsite, arity check passes or we have an ArgumentError
            int numArgs = ii.getArgsCount();
            if ((numArgs < required) || ((rest == -1) && (numArgs > (required + opt)))) {
                return new RaiseArgumentErrorInstr(required, opt, rest, rest);
            }

            return null;
        } else {
            return new CheckArgsArrayArityInstr(ii.getArgs(), required, opt, rest);
        }
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new CheckArityInstr(required, opt, rest);
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    }
}
