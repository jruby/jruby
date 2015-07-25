package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import java.util.Map;

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
    private final String scopeName; // Primarily a debugging aid

    public BreakInstr(Operand returnValue, String scopeName) {
        super(Operation.BREAK, new Operand[] { returnValue });
        this.scopeName = scopeName;
    }

    public String getScopeName() {
        return scopeName;
    }

    public Operand getReturnValue() {
        return operands[0];
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.HAS_BREAK_INSTRS);
        scope.getFlags().add(IRFlags.REQUIRES_DYNSCOPE);
        return true;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"scope_name: " + scopeName};
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new BreakInstr(getReturnValue().cloneForInlining(info), scopeName);

        InlineCloneInfo ii = (InlineCloneInfo) info;

        if (ii.isClosure()) {
            // SSS FIXME: This is buggy!
            //
            // If scopeIdToReturnTo is a closure, it could have
            // been cloned as well!! This is only an issue if we
            // inline in closures. But, if we always inline in methods,
            // this will continue to work.
            //
            // Hmm ... we need to figure out the required inlining info here.
            //
            // if (ii.getHostScope().getScopeId() == scopeIdToReturnTo) {
            //
            if (false) {
                // If the break got inlined into the scope we had to break to, replace the break
                // with a COPY of the break-value into the call's result var.
                // Ex: v = foo { ..; break n; ..}.  So, "break n" is replaced with "v = n"
                // The CFG for the closure will be such that after break, control goes to the
                // scope exit block.  So, we know that after the copy, we'll continue with the
                // instruction after the call.
                Variable v = ii.getCallResultVariable();
                return (v == null) ? null : new CopyInstr(v, getReturnValue().cloneForInlining(ii));
            }

            return new BreakInstr(getReturnValue().cloneForInlining(ii), scopeName);
        } else {
            throw new UnsupportedOperationException("Break instructions shouldn't show up outside closures.");
        }
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReturnValue());
        e.encode(getScopeName());
    }

    public static BreakInstr decode(IRReaderDecoder d) {
        return new BreakInstr(d.decodeOperand(), d.decodeString());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BreakInstr(this);
    }
}
