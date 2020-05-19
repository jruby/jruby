package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This instruction shows only when a block is inlined.
// Opt arg receive instructions get transformed to this.
// This does not show up in regular Ruby code.
public class OptArgMultipleAsgnInstr extends MultipleAsgnBase implements FixedArityInstr {
    /** This instruction gets to pick an argument off the arry only if
     *  the array has at least these many elements */
    private final int minArgsLength;

    public OptArgMultipleAsgnInstr(Variable result, Operand array, int index, int minArgsLength) {
        super(Operation.MASGN_OPT, result, array, index);
        this.minArgsLength = minArgsLength;
    }

    public int getMinArgsLength() {
        return minArgsLength;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "index: " + index, "min_length: " + minArgsLength};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OptArgMultipleAsgnInstr(ii.getRenamedVariable(result), getArray().cloneForInlining(ii), index, minArgsLength);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArray());
        e.encode(getIndex());
        e.encode(getMinArgsLength());
    }

    public static OptArgMultipleAsgnInstr decode(IRReaderDecoder d) {
        return new OptArgMultipleAsgnInstr(d.decodeVariable(), d.decodeOperand(), d.decodeInt(), d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray rubyArray = (RubyArray) getArray().retrieve(context, self, currScope, currDynScope, temp);
        return IRRuntimeHelpers.extractOptionalArgument(rubyArray, minArgsLength, index);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.OptArgMultipleAsgnInstr(this);
    }
}
