package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StoreToFrameInstr extends PutInstr {
    public StoreToFrameInstr(IRExecutionScope scope, String slotName, Operand value) {
        super(Operation.FRAME_STORE, MetaObject.create(getClosestMethodAncestor(scope)), slotName, value);
    }

    private static IRMethod getClosestMethodAncestor(IRExecutionScope scope) {
        while (!(scope instanceof IRMethod)) {
            scope = (IRExecutionScope)scope.getLexicalParent();
        }

        return (IRMethod) scope;
    }

    @Override
    public String toString() {
        return "\tFRAME(" + operands[TARGET] + ")." + ref + " = " + operands[VALUE];
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new StoreToFrameInstr((IRExecutionScope)((MetaObject)operands[TARGET]).scope, ref, operands[VALUE].cloneForInlining(ii));
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
