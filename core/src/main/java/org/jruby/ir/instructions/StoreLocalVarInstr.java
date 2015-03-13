package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StoreLocalVarInstr extends Instr implements FixedArityInstr {
    private final IRScope scope;

    public StoreLocalVarInstr(Operand value, IRScope scope, LocalVariable lvar) {
        super(Operation.BINDING_STORE, new Operand[] { value, lvar });

        this.scope = scope;
    }


    public Operand getValue() {
        return operands[0];
    }

    /** This is the variable that is being stored into in this scope.  This variable
     * doesn't participate in the computation itself.  We just use it as a proxy for
     * its (a) name (b) offset (c) scope-depth. */
    public LocalVariable getLocalVar() {
        return (LocalVariable) operands[1];
    }

    public IRScope getScope() {
        return scope;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "scope_name: " + scope.getName()};
    }

    // SSS FIXME: This feels dirty
    public void decrementLVarScopeDepth() {
        operands[1] = getLocalVar().cloneForDepth(getLocalVar().getScopeDepth()-1);
    }

    /**
     * getLocalVar is saved for location and should not be simplified so we still know its original
     * depth/offset.
     */
    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        operands[0] = getValue().getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        // SSS FIXME: Do we need to rename lvar really?  It is just a name-proxy!
        return new StoreLocalVarInstr(getValue().cloneForInlining(ii), scope,
                (LocalVariable) getLocalVar().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getScope());
        e.encode(getLocalVar());
        e.encode(getValue());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object varValue = getValue().retrieve(context, self, currScope, currDynScope, temp);
        currDynScope.setValue((IRubyObject)varValue, getLocalVar().getLocation(), getLocalVar().getScopeDepth());
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.StoreLocalVarInstr(this);
    }
}
