package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveSelfInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;

    // SSS FIXME: destination always has to be a local variable '%self'.  So, is this a redundant arg?
    public ReceiveSelfInstr(Variable result) {
        super(Operation.RECV_SELF);

        assert result != null: "ReceiveSelfInstr result is null";

        this.result = result;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
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
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return this;

        // receive-self will disappear after inlining and all uses of %self will be replaced by the call receiver
        // FIXME: What about 'self' in closures??
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveSelfInstr(this);
    }
}
