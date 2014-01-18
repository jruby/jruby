package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

import java.util.Map;
import org.jruby.ir.operands.ScopeModule;

// NOTE: breaks that jump out of while/until loops would have
// been transformed by the IR building into an ordinary jump.
//
// A break instruction is not just any old instruction.
// Like a return instruction, it exits a scope and returns a value
//
// Ex: (1..5).collect { |n| break if n > 3; n } returns nil
//
// All break instructions like returns have an associated return value
// In the absence of an explicit value to return, nil is returned
//
// Ex: (1..5).collect { |n| break "Hurrah" if n > 3; n } returns "Hurrah"
//
// But, whereas a return exits the innermost method it is in,
// a break only exits out of the innermost non-method scope it is in.
// So, an exposed/naked break inside a method throws an exception!
//
// def foo(n); break if n > 5; end; foo(100) will throw an exception
//
public class BreakInstr extends Instr implements FixedArityInstr {
    private final IRScope scopeToReturnTo;
    private Operand returnValue;

    public BreakInstr(Operand rv, IRScope s) {
        super(Operation.BREAK);
        this.scopeToReturnTo = s;
        this.returnValue = rv;
    }

    public IRScope getScopeToReturnTo() {
        return scopeToReturnTo;
    }

    public Operand getReturnValue() {
        return returnValue;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new ScopeModule(scopeToReturnTo), returnValue };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case CLOSURE_INLINE:
                // SSS FIXME: This is buggy!
                //
                // If scopeToReturnTo is a closure, it could have
                // been cloned as well!! This is only an issue if we
                // inline in closures. But, if we always inline in methods,
                // this will continue to work.
                if (ii.getInlineHostScope() == scopeToReturnTo) {
                    // If the break got inlined into the scope we had to break to, replace the break
                    // with a COPY of the break-value into the call's result var.
                    // Ex: v = foo { ..; break n; ..}.  So, "break n" is replaced with "v = n"
                    // The CFG for the closure will be such that after break, control goes to the
                    // scope exit block.  So, we know that after the copy, we'll continue with the
                    // instruction after the call.
                    Variable v = ii.getCallResultVariable();
                    return (v == null) ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
                }
                // fall through
            case NORMAL_CLONE:
                return new BreakInstr(returnValue.cloneForInlining(ii), scopeToReturnTo);
            default:
                return super.cloneForInlining(ii);
        }
    }

    @Override
    public String toString() {
        return getOperation() + "(" + returnValue + (scopeToReturnTo == null ? "" : ", " + scopeToReturnTo) + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        returnValue = returnValue.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BreakInstr(this);
    }
}
