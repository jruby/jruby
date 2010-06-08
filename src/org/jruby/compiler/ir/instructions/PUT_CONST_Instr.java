package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class PUT_CONST_Instr extends PutInstr
{
    public PUT_CONST_Instr(IRScope scope, String constName, Operand val) {
        super(Operation.PUT_CONST, new MetaObject(scope), constName, val);
    }

    public PUT_CONST_Instr(Operand scopeOrObj, String constName, Operand val) {
        super(Operation.PUT_CONST, scopeOrObj, constName, val);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PUT_CONST_Instr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }
}
