package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ArgScopeDepthInstr extends ResultBaseInstr implements FixedArityInstr {
    public ArgScopeDepthInstr(Variable result) {
        super(Operation.ARG_SCOPE_DEPTH, result, EMPTY_OPERANDS);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ArgScopeDepthInstr(ii.getRenamedVariable(result));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ArgScopeDepthInstr(this);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        int i = 0;
        while (!currDynScope.getStaticScope().isArgumentScope()) {
            currDynScope = currDynScope.getParentScope();
            i++;
        }
        return context.runtime.newFixnum(i);
    }

    public static ArgScopeDepthInstr decode(IRReaderDecoder d) {
        return new ArgScopeDepthInstr(d.decodeVariable());
    }
}
