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

public class StoreToBindingInstr extends PutInstr {
    private int bindingSlot;
    public StoreToBindingInstr(IRExecutionScope scope, String slotName, Operand value) {
        super(Operation.BINDING_STORE, MetaObject.create(scope.getClosestMethodAncestor()), slotName, value);

        MetaObject mo = (MetaObject)getTarget();
        IRMethod m = (IRMethod)mo.scope;
        bindingSlot = m.assignBindingSlot(slotName);
    }

    @Override
    public String toString() {
        return "\tBINDING(" + operands[TARGET] + ")." + ref + " = " + operands[VALUE];
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new StoreToBindingInstr((IRExecutionScope)((MetaObject)operands[TARGET]).scope, ref, operands[VALUE].cloneForInlining(ii));
    }

    private IRScope getIRScope(Operand scopeHolder) {
        assert scopeHolder instanceof MetaObject : "Target should be a MetaObject";

        return ((MetaObject) scopeHolder).getScope();
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
		  LocalVariable v = (LocalVariable) getValue();
        IRMethod m = (IRMethod)getIRScope(getTarget());
        if (bindingSlot == -1)
            bindingSlot = m.getBindingSlot(v.getName());
        interp.setSharedBindingVariable(bindingSlot, interp.getLocalVariable(v.getLocation()));
        return null;
    }
}
