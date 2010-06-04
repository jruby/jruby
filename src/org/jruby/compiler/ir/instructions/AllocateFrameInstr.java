package org.jruby.compiler.ir.instructions;

/**
 * This instructions allocates a heap frame for the current execution scope.
 * Does nothing if a frame already exists.
 **/

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.Map;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AllocateFrameInstr extends Instr {
    IR_ExecutionScope scope;   // Scope for which frame is needed

    public AllocateFrameInstr(IR_ExecutionScope scope) {
        super(Operation.ALLOC_FRAME);
        
        this.scope = getClosestMethodAncestor(scope);
    }

    // ENEBO: Should we be reallocing this every time?
    public Operand[] getOperands() { 
        return new Operand[] { new MetaObject(scope) };
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {}

    private static IRMethod getClosestMethodAncestor(IR_ExecutionScope scope) {
        while (!(scope instanceof IRMethod)) {
            scope = (IR_ExecutionScope)scope.getLexicalParent();
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
    public void interpret(InterpreterContext interp, IRubyObject self) {
        interp.getContext().pushFrame();
        interp.setFrame(interp.getContext().getCurrentFrame());
    }
}
