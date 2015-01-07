package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

// Represents a splat value in Ruby code: *array
public class BuildSplatInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand array;

    public BuildSplatInstr(Variable result, Operand array) {
        super(Operation.BUILD_SPLAT);
        this.result = result;
        this.array = array;
    }

    @Override
    public String toString() {
        return result + " = *" + array;
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand getArray() {
        return array;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { array };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        array = array.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildSplatInstr(ii.getRenamedVariable(result), array.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject arrayVal = (IRubyObject) array.retrieve(context, self, currScope, currDynScope, temp);
        return IRRuntimeHelpers.irSplat(context, arrayVal);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildSplatInstr(this);
    }
}
