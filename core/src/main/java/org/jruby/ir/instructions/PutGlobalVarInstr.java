package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutGlobalVarInstr extends Instr implements FixedArityInstr {
    public PutGlobalVarInstr(String varName, Operand value) {
        this(new GlobalVariable(varName), value);
    }

    public PutGlobalVarInstr(GlobalVariable gvar, Operand value) {
        super(Operation.PUT_GLOBAL_VAR, new Operand[] {gvar, value});
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        String name = getTarget().getName();

        if (name.equals("$_") || name.equals("$~")) {
            scope.getFlags().add(IRFlags.USES_BACKREF_OR_LASTLINE);
            return true;
        }

        return false;
    }

    public GlobalVariable getTarget() {
        return (GlobalVariable) operands[0];
    }

    public Operand getValue() {
        return operands[1];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new PutGlobalVarInstr(getTarget().getName(), getValue().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getTarget());
        e.encode(getValue());
    }

    public static PutGlobalVarInstr decode(IRReaderDecoder d) {
        return new PutGlobalVarInstr((GlobalVariable) d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        GlobalVariable target = getTarget();
        IRubyObject    value  = (IRubyObject) getValue().retrieve(context, self, currScope, currDynScope, temp);
        context.runtime.getGlobalVariables().set(target.getName(), value);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutGlobalVarInstr(this);
    }
}
