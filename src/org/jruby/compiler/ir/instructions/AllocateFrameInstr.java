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

public class AllocateFrameInstr extends Instr {
    IRExecutionScope scope;   // Scope for which frame is needed

    public AllocateFrameInstr(IRExecutionScope scope) {
        super(Operation.ALLOC_FRAME);
        
        this.scope = getClosestMethodAncestor(scope);
    }

    // ENEBO: Should we be reallocing this every time?
    public Operand[] getOperands() { 
        return new Operand[] { MetaObject.create(scope) };
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {}

    private static IRMethod getClosestMethodAncestor(IRExecutionScope scope) {
        while (!(scope instanceof IRMethod)) {
            scope = (IRExecutionScope)scope.getLexicalParent();
        }

        return (IRMethod)scope;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        // The frame will now be allocated in the caller's scope
        return new AllocateFrameInstr(ii.callerCFG.getScope());
    }

    @Override
    public String toString() {
        return "\t" + operation + "(" + scope + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        // ENEBO: This is slightly better than pushFrame but at least we are pushing proper self, block,
        // and static scope.  The impl class may or may not be correct.
        RubyModule implementationClass = scope.getStaticScope().getModule();

        if (implementationClass == null) {
            implementationClass = interp.getRuntime().getObject();
        }

        interp.getContext().preMethodFrameAndScope(implementationClass, null, self,
                interp.getBlock(), scope.getStaticScope());
//        interp.getContext().pushFrame();
        interp.setFrame(interp.getContext().getCurrentFrame());
        return null;
    }
}
