package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class LineNumberInstr extends Instr implements FixedArityInstr {
    public final int lineNumber;
    public final IRScope scope; // We need to keep scope info here so that line number is meaningful across inlinings.

    public LineNumberInstr(IRScope scope, int lineNumber) {
        super(Operation.LINE_NUM);
        this.scope = scope;
        this.lineNumber = lineNumber;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new ScopeModule(scope), new Fixnum(lineNumber) };
    }

    @Override
    public String toString() {
        return super.toString() + "(" + lineNumber + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: This is buggy! 'scope' might have changed because of cloning.
        return this;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LineNumberInstr(this);
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
