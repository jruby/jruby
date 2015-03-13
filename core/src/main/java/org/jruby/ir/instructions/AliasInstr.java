package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class AliasInstr extends Instr implements FixedArityInstr {
    // SSS FIXME: Implicit self arg -- make explicit to not get screwed by inlining!
    public AliasInstr(Operand newName, Operand oldName) {
        super(Operation.ALIAS, new Operand[] {newName, oldName});
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.REQUIRES_DYNSCOPE);
        return true;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);

        e.encode(getNewName());
        e.encode(getOldName());
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new AliasInstr(getNewName().cloneForInlining(ii), getOldName().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        String newNameString = getNewName().retrieve(context, self, currScope, currDynScope, temp).toString();
        String oldNameString = getOldName().retrieve(context, self, currScope, currDynScope, temp).toString();
        IRRuntimeHelpers.defineAlias(context, self, currDynScope, newNameString, oldNameString);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AliasInstr(this);
    }

    public Operand getNewName() {
        return operands[0];
    }

    public Operand getOldName() {
        return operands[1];
    }
}
