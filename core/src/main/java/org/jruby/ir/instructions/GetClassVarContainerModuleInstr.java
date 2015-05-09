package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Finds the module that will hold class vars for the object that is being queried.
 * A candidate static IRMethod is also passed in.
 */
// SSS FIXME: Split into 2 different instrs?
// CON: Only appears to use self, so we can just early eval and use same logic
public class GetClassVarContainerModuleInstr extends ResultBaseInstr implements FixedArityInstr {
    public GetClassVarContainerModuleInstr(Variable result, Operand startingScope, Variable object) {
        super(Operation.CLASS_VAR_MODULE, result, object == null ? new Operand[] {startingScope} : new Operand[] {startingScope, object});

        assert result != null;
    }

    public Variable getObject() {
        return (Variable) (operands.length >= 2 ? operands[1] : null);
    }

    public Operand getStartingScope() {
        return operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GetClassVarContainerModuleInstr(ii.getRenamedVariable(result),
                getStartingScope().cloneForInlining(ii),
                getObject() == null ? null : (Variable) getObject().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getStartingScope());
        e.encode(getObject());
    }

    public static GetClassVarContainerModuleInstr decode(IRReaderDecoder d) {
        return new GetClassVarContainerModuleInstr(d.decodeVariable(), d.decodeOperand(), d.decodeVariable());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        StaticScope scope = (StaticScope) getStartingScope().retrieve(context, self, currScope, currDynScope, temp);
        Operand object = getObject();
        IRubyObject arg = object == null ? null : (IRubyObject) object.retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.getModuleFromScope(context, scope, arg);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetClassVarContainerModuleInstr(this);
    }
}
