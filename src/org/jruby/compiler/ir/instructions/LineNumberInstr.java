package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class LineNumberInstr extends NoOperandInstr
{
    public final int lineNumber;
    public final IR_Scope scope; // We need to keep scope info here so that line number is meaningful across inlinings.

    public LineNumberInstr(IR_Scope scope, int lineNumber) {
        super(Operation.LINE_NUM);
        this.scope = scope;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() { return super.toString() + "(" + lineNumber + ")"; }

    public Instr cloneForInlining(InlinerInfo ii) { return this; }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        interp.getContext().setLine(lineNumber);
    }
}
