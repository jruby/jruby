package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.operands.LocalVariable;
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
        return "\tSTORE_TO_FRAME(" + operands[TARGET] + ")." + ref + " = " + operands[VALUE];
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new StoreToFrameInstr((IRExecutionScope)((MetaObject)operands[TARGET]).scope, ref, operands[VALUE].cloneForInlining(ii));
    }

    private IRScope getIRScope(Operand scopeHolder) {
        assert scopeHolder instanceof MetaObject : "Target should be a MetaObject";

        return ((MetaObject) scopeHolder).getScope();
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Operand var = getValue();

        assert var instanceof LocalVariable;

        String name = ((LocalVariable) var).getName();
        interp.setFrameVariable(getIRScope(getTarget()), name, interp.getLocalVariable(name));
        return null;
    }
}
