package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetGlobalVariableInstr extends GetInstr  implements FixedArityInstr {
    public GetGlobalVariableInstr(Variable dest, String gvarName) {
        this(dest, new GlobalVariable(gvarName));
    }

    public GetGlobalVariableInstr(Variable dest, GlobalVariable gvar) {
        super(Operation.GET_GLOBAL_VAR, dest, gvar, null);
    }

    public boolean computeScopeFlags(IRScope scope) {
        String name = ((GlobalVariable) getSource()).getName();

        if (name.equals("$_") || name.equals("$~") || name.equals("$`") || name.equals("$'") ||
            name.equals("$+") || name.equals("$LAST_READ_LINE") || name.equals("$LAST_MATCH_INFO") ||
            name.equals("$PREMATCH") || name.equals("$POSTMATCH") || name.equals("$LAST_PAREN_MATCH")) {
            scope.getFlags().add(IRFlags.USES_BACKREF_OR_LASTLINE);
            return true;
        }

        return false;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GetGlobalVariableInstr(ii.getRenamedVariable(getResult()), ((GlobalVariable)getSource()).getName());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return getSource().retrieve(context, self, currScope, currDynScope, temp);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetGlobalVariableInstr(this);
    }
}
