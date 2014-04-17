package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

/*
 * Finds the module that will hold class vars for the object that is being queried.
 * A candidate static IRMethod is also passed in.
 */
// SSS FIXME: Split into 2 different instrs?
// CON: Only appears to use self, so we can just early eval and use same logic
public class GetClassVarContainerModuleInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Operand  startingScope;
    // needs to be side-effect free for simpler logic below
    private Variable  object;
    private Variable result;

    public GetClassVarContainerModuleInstr(Variable result, Operand startingScope, Variable object) {
        super(Operation.CLASS_VAR_MODULE);

        assert result != null;

        this.startingScope = startingScope;
        this.object = object;
        this.result = result;
    }

    public Variable getObject() {
        return object;
    }

    public Operand getStartingScope() {
        return startingScope;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetClassVarContainerModuleInstr(ii.getRenamedVariable(result), startingScope.cloneForInlining(ii), object == null ? null : (Variable)object.cloneForInlining(ii));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + startingScope + ", " + object + ")";
    }

    @Override
    public Operand[] getOperands() {
        return object == null ? new Operand[] {startingScope} : new Operand[] {startingScope, object};
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        startingScope = startingScope.getSimplifiedOperand(valueMap, force);
        if (object != null) object = (Variable)object.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Ruby        runtime   = context.runtime;
        StaticScope scope     = (StaticScope) startingScope.retrieve(context, self, currDynScope, temp);
        IRubyObject arg =
                object == null ?
                        null :
                        (IRubyObject) object.retrieve(context, self, currDynScope, temp);

        return IRRuntimeHelpers.getModuleFromScope(context, scope, arg);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetClassVarContainerModuleInstr(this);
    }
}
