package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * All variables are allocated in the binding of the nearest method ancestor.  Additionally, all variables 
 * with the same name in all closures (however deeply nested) get a single shared slot in the method's binding.
 *
 * So, when we encounter a load from binding instruction in some execution scope, we traverse the scope
 * tree till we find a method.  We are guaranteed to find one since closures dont float free --
 * they are always tethered to a surrounding scope!  This also means that we can find the neareast
 * method ancestor by simply traversing lexical scopes -- no need to traverse the dynamic scopes
 *
 * SSS FIXME: except perhaps when we use class_eval, module_eval, or instance_eval??
 */

public class LoadFromBindingInstr extends Instr {
    private IRMethod sourceMethod;
    private int      bindingSlot;
    private String   slotName;

    public LoadFromBindingInstr(Variable v, IRExecutionScope scope, String slotName) {
        super(Operation.BINDING_LOAD, v);

        this.slotName = slotName;
        this.sourceMethod = (IRMethod)scope.getClosestMethodAncestor();
        bindingSlot = sourceMethod.assignBindingSlot(slotName);
    }

    public String getSlotName() {
        return slotName;
    }

    public Operand[] getOperands() { 
        return new Operand[] { };
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        /* Nothing to do */
    }

    @Override
    public String toString() {
        return "\t" + result + " = BINDING(" + sourceMethod + ")." + getSlotName();
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new LoadFromBindingInstr(ii.getRenamedVariable(result), sourceMethod, getSlotName());
    }

    // Any exception raised by the execution of this instruction is an interpreter/compiler bug
    @Override
    public boolean canRaiseException() { return false; }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        LocalVariable v = (LocalVariable)getResult();
        if (bindingSlot == -1)
            bindingSlot = sourceMethod.getBindingSlot(getSlotName());
        interp.setLocalVariable(v.getLocation(), interp.getSharedBindingVariable(bindingSlot));
        return null;
    }
}
