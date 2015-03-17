package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GVarAliasInstr extends Instr implements FixedArityInstr {
    public GVarAliasInstr(Operand newName, Operand oldName) {
        super(Operation.GVAR_ALIAS, new Operand[] { newName, oldName });
    }

    public Operand getNewName() {
        return operands[0];
    }

    public Operand getOldName() {
        return operands[1];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GVarAliasInstr(getNewName().cloneForInlining(ii), getOldName().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getNewName());
        e.encode(getOldName());
    }

    public static GVarAliasInstr decode(IRReaderDecoder d) {
        return new GVarAliasInstr(d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        String newNameString = getNewName().retrieve(context, self, currScope, currDynScope, temp).toString();
        String oldNameString = getOldName().retrieve(context, self, currScope, currDynScope, temp).toString();

        context.runtime.getGlobalVariables().alias(newNameString, oldNameString);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GVarAliasInstr(this);
    }
}
