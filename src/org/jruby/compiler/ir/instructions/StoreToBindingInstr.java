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

public class StoreToBindingInstr extends OneOperandInstr {
	 private IRMethod targetMethod;
    private int      bindingSlot;
    private String   slotName;

    public StoreToBindingInstr(IRExecutionScope scope, String slotName, Operand value) {
        super(Operation.BINDING_STORE, null, value);

        this.slotName = slotName;
		  this.targetMethod = (IRMethod)scope.getClosestMethodAncestor();
        bindingSlot = targetMethod.assignBindingSlot(slotName);
    }

    public String getSlotName() {
        return slotName;
    }

    @Override
    public String toString() {
        return "\tBINDING(" + targetMethod + ")." + slotName + " = " + getArg();
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new StoreToBindingInstr(targetMethod, slotName, getArg().cloneForInlining(ii));
    }

    // Any exception raised by the execution of this instruction is an interpreter/compiler bug
    @Override
    public boolean canRaiseException() { return false; }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
		  LocalVariable v = (LocalVariable) getArg();
        if (bindingSlot == -1)
            bindingSlot = targetMethod.getBindingSlot(v.getName());
        interp.setSharedBindingVariable(bindingSlot, interp.getLocalVariable(v.getLocation()));
        return null;
    }
}
