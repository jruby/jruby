package org.jruby.compiler.ir.instructions;

/**
 * This instructions allocates a heap frame for the current execution scope.
 * Does nothing if a frame already exists.
 **/

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AllocateBindingInstr extends Instr {
    private IRMethod scope;   // Scope for which frame is needed

    public AllocateBindingInstr(IRExecutionScope scope) {
        super(Operation.ALLOC_BINDING);
        
        this.scope = scope.getClosestMethodAncestor();
    }

    // ENEBO: Should we be reallocing this every time?
    public Operand[] getOperands() { 
        return new Operand[] { MetaObject.create(scope) };
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        // The frame will now be allocated in the caller's scope
        return new AllocateBindingInstr(ii.callerCFG.getScope());
    }

    // Can this instruction raise exceptions?
	 // If this instruction raises an exception, you are in deep doo-doo.
    @Override
    public boolean canRaiseException() {
        return false;
    }

    @Override
    public String toString() {
        return "" + getOperation() + "(" + scope + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
/**
 * SSS: This is going to be a NO-OP in the current implementation because of the existing JRuby runtime
 * is structure.  ThreadContext accesses static-scope via a DynamicScope!  This means it expects a
 * dynamic scope to be allocated for every method context.  So, we cannot make use of our conditional
 * binding allocation as implemented by this instruction.  In the future, when the runtime is restructured
 * to separate static and dynamic scopes, we can revive this again.
 *
        // The impl class may or may not be correct.
        RubyModule implementationClass = scope.getStaticScope().getModule();
        if (implementationClass == null) {
            implementationClass = interp.getRuntime().getObject();
        }
        interp.allocateSharedBindingScope(scope);
**/
        return null;
    }
}
