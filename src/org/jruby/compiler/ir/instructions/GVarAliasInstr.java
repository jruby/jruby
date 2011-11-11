package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class GVarAliasInstr extends Instr {
    private final Operand newName;
    private final Operand oldName;

    public GVarAliasInstr(Operand newName, Operand oldName) {
        super(Operation.GVAR_ALIAS);
        
        this.newName = newName;
        this.oldName = oldName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { newName, oldName };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GVarAliasInstr(newName.cloneForInlining(ii), oldName.cloneForInlining(ii));
    }

    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block, Object exception, Object[] temp) {
        String newNameString = newName.retrieve(interp, context, self, temp).toString();
        String oldNameString = oldName.retrieve(interp, context, self, temp).toString();

        context.getRuntime().getGlobalVariables().alias(newNameString, oldNameString);
        return null;
    }
}
