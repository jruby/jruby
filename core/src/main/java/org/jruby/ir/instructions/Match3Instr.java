/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

import static org.jruby.ir.IRFlags.USES_BACKREF_OR_LASTLINE;

public class Match3Instr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;
    private Operand receiver;
    private Operand arg;

    public Match3Instr(Variable result, Operand receiver, Operand arg) {
        super(Operation.MATCH3);

        assert result != null: "Match3Instr result is null";

        this.result = result;
        this.receiver = receiver;
        this.arg = arg;
    }

    public Operand getArg() {
        return arg;
    }

    public Operand getReceiver() {
        return receiver;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { receiver, arg };
    }

    @Override
    public String toString() {
        return super.toString() + "(" + receiver + ", " + arg + ")";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(USES_BACKREF_OR_LASTLINE);
        return true;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        receiver = receiver.getSimplifiedOperand(valueMap, force);
        arg = arg.getSimplifiedOperand(valueMap, force);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new Match3Instr((Variable) result.cloneForInlining(ii),
                receiver.cloneForInlining(ii), arg.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        RubyRegexp regexp = (RubyRegexp) receiver.retrieve(context, self, currDynScope, temp);
        IRubyObject argValue = (IRubyObject) arg.retrieve(context, self, currDynScope, temp);

        return IRRuntimeHelpers.match3(context, regexp, argValue);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Match3Instr(this);
    }
}
