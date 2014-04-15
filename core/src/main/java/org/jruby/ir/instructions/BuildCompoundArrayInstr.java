package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

import java.util.List;
import java.util.Map;

// This represents an array that is used solely during arguments construction
//   * Array + Splat ([1,2,3], *[5,6,7])
// This used to be an operand, but since to_a can be called as part of
// building the args-cat/push value, this is not really side-effect free.
public class BuildCompoundArrayInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand a1;
    private Operand a2;
    private boolean isArgsPush;

    public BuildCompoundArrayInstr(Variable result, Operand a1, Operand a2, boolean isArgsPush) {
        super(Operation.BUILD_COMPOUND_ARRAY);
        this.a1 = a1;
        this.a2 = a2;
        this.result = result;
        this.isArgsPush = isArgsPush;
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand getA1() {
        return a1;
    }

    public Operand getA2() {
        return a2;
    }

    // SSS FIXME: Consolidate identical methods
    public Operand getAppendingArg() {
        return a1;
    }

    // SSS FIXME: Consolidate identical methods
    public Operand getAppendedArg() {
        return a2;
    }

    public boolean isArgsPush() { return isArgsPush; }

    @Override
    public Operand[] getOperands() {
        return new Operand[] {a1, a2};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        a1 = a1.getSimplifiedOperand(valueMap, force);
        a2 = a2.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BuildCompoundArrayInstr(ii.getRenamedVariable(result), a1.cloneForInlining(ii), a2.cloneForInlining(ii), isArgsPush);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject v1 = (IRubyObject)a1.retrieve(context, self, currDynScope, temp);
        IRubyObject v2 = (IRubyObject)a2.retrieve(context, self, currDynScope, temp);
        return isArgsPush ? Helpers.argsPush((RubyArray) v1, v2) : Helpers.argsCat(v1, v2);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildCompoundArrayInstr(this);
    }

    @Override
    public String toString() {
        return result + " = " + (isArgsPush ? "ArgsPush[" : "ArgsCat:[") + a1 + ", " + a2 + "]";
    }
}
