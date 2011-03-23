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

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AllocateBindingInstr extends Instr {
    IRMethod scope;   // Scope for which frame is needed

    public AllocateBindingInstr(IRExecutionScope scope) {
        super(Operation.ALLOC_BINDING);
        
        this.scope = scope.getClosestMethodAncestor();
    }

    // ENEBO: Should we be reallocing this every time?
    public Operand[] getOperands() { 
        return new Operand[] { MetaObject.create(scope) };
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {}

    public Instr cloneForInlining(InlinerInfo ii) {
        // The frame will now be allocated in the caller's scope
        return new AllocateBindingInstr(ii.callerCFG.getScope());
    }

    // Can this instruction raise exceptions?
	 // If this instruction raises an exception, you are in deep doo-doo.
    @Override
    public boolean canRaiseException() { return false; }

    @Override
    public String toString() {
        return "\t" + operation + "(" + scope + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        // The impl class may or may not be correct.
        RubyModule implementationClass = scope.getStaticScope().getModule();

        if (implementationClass == null) {
            implementationClass = interp.getRuntime().getObject();
        }

        interp.allocateSharedBindingScope(scope);
        return null;
    }
}
