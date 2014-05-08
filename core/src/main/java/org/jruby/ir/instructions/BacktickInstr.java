package org.jruby.ir.instructions;

import org.jruby.RubyBasicObject;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// This represents a backtick string in Ruby
// Ex: `ls .`; `cp #{src} #{dst}`
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
public class BacktickInstr extends Instr implements ResultInstr {
    private Variable result;
    private List<Operand> pieces;

    public BacktickInstr(Variable result, Operand val) {
        super(Operation.BACKTICK_STRING);
        this.result = result;
        pieces = new ArrayList<Operand>();
        pieces.add(val);
    }

    public BacktickInstr(Variable result, List<Operand> pieces) {
        super(Operation.BACKTICK_STRING);
        this.result = result;
        this.pieces = pieces;
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
    public Operand[] getOperands() {
        return pieces.toArray(new Operand[pieces.size()]);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        List<Operand> newPieces = new ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.getSimplifiedOperand(valueMap, force));
        }

       pieces = newPieces;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        List<Operand> newPieces = new ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.cloneForInlining(ii));
        }

        return new BacktickInstr(ii.getRenamedVariable(result), newPieces);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        RubyString newString = context.runtime.newString();

        for (Operand p: pieces) {
            RubyBasicObject piece = (RubyBasicObject) p.retrieve(context, self, currDynScope, temp);
            newString.append((piece instanceof RubyString) ? (RubyString) piece : piece.to_s());
        }

        return self.callMethod(context, "`", newString);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BacktickInstr(this);
    }

    @Override
    public String toString() {
        return result + "`" + (pieces == null ? "[]" : pieces) + "`";
    }
}
