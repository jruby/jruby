package org.jruby.ir.instructions;

import org.jruby.RubyBasicObject;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This represents a backtick string in Ruby
// Ex: `ls .`; `cp #{src} #{dst}`
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
public class BacktickInstr extends ResultBaseInstr {
    public BacktickInstr(Variable result, Operand[] pieces) {
        super(Operation.BACKTICK_STRING, result, pieces);
    }

    public Operand[] getPieces() {
        return getOperands();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BacktickInstr(ii.getRenamedVariable(result), cloneOperands(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getPieces());
    }

    public static BacktickInstr decode(IRReaderDecoder d) {
        return new BacktickInstr(d.decodeVariable(), d.decodeOperandArray());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        RubyString newString = context.runtime.newString();

        for (Operand p: operands) {
            RubyBasicObject piece = (RubyBasicObject) p.retrieve(context, self, currScope, currDynScope, temp);
            newString.append((piece instanceof RubyString) ? (RubyString) piece : piece.to_s());
        }

        newString.setFrozen(true);

        return self.callMethod(context, "`", newString);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BacktickInstr(this);
    }
}
