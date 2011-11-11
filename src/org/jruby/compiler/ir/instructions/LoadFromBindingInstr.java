package org.jruby.compiler.ir.instructions;


import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
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

public class LoadFromBindingInstr extends Instr implements ResultInstr {
    private IRMethod sourceMethod;
    private int bindingSlot;
    private String slotName;
    private Variable result;

    public LoadFromBindingInstr(Variable result, IRExecutionScope scope, String slotName) {
        super(Operation.BINDING_LOAD);

        assert result != null: "LoadFromBindingInstr result is null";
        
        this.slotName = slotName;
        this.sourceMethod = (IRMethod)scope.getClosestMethodAncestor();
        bindingSlot = sourceMethod.assignBindingSlot(slotName);
        this.result = result;
    }

    public String getSlotName() {
        return slotName;
    }

    public Operand[] getOperands() { 
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }
    @Override
    public String toString() {
        return "" + result + " = BINDING(" + sourceMethod + ")." + getSlotName();
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new LoadFromBindingInstr(ii.getRenamedVariable(result), sourceMethod, getSlotName());
    }

    // Any exception raised by the execution of this instruction is an interpreter/compiler bug
    @Override
    public boolean canRaiseException() {
        return false;
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        LocalVariable v = (LocalVariable) result;
        
        if (bindingSlot == -1) bindingSlot = sourceMethod.getBindingSlot(getSlotName());
        int depth = 0; // All binding slots are in the top-most scope
        DynamicScope variableScope = context.getCurrentScope();
        
        variableScope.setValue(v.getLocation(), variableScope.getValue(bindingSlot, depth), v.getScopeDepth());
        
        return null;
    }
}
