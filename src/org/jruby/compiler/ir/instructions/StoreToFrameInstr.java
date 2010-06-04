package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StoreToFrameInstr extends PutInstr {
    public StoreToFrameInstr(IR_ExecutionScope scope, String slotName, Operand value) {
        super(Operation.FRAME_STORE, new MetaObject(getClosestMethodAncestor(scope)), slotName, value);
    }

    private static IRMethod getClosestMethodAncestor(IR_ExecutionScope scope) {
        while (!(scope instanceof IRMethod)) {
            scope = (IR_ExecutionScope)scope.getLexicalParent();
        }

        return (IRMethod) scope;
    }

    @Override
    public String toString() {
        return "\tFRAME(" + operands[TARGET] + ")." + ref + " = " + operands[VALUE];
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new StoreToFrameInstr((IR_ExecutionScope)((MetaObject)operands[TARGET]).scope, ref, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        if (getName().equals("self")) {
            interp.getFrame().setSelf(self);
        } else {
            // Our lvars are actually using the same backign store as frame params
        }
    }
}
