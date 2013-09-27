package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This instruction shows only when a block is inlined.
// Opt arg receive instructions get transformed to this.
// This does not show up in regular Ruby code.
public class OptArgMultipleAsgnInstr extends MultipleAsgnBase {
    /** This instruction gets to pick an argument off the arry only if
     *  the array has at least these many elements */
    private final int minArgsLength;

    public OptArgMultipleAsgnInstr(Variable result, Operand array, int index, int minArgsLength) {
        super(Operation.MASGN_OPT, result, array, index);
        this.minArgsLength = minArgsLength;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + array + "," + index + "," + minArgsLength + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new OptArgMultipleAsgnInstr(ii.getRenamedVariable(result), array.cloneForInlining(ii), index, minArgsLength);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray rubyArray = (RubyArray) array.retrieve(context, self, currDynScope, temp);
        Object val;

        int n = rubyArray.getLength();
		  return minArgsLength <= n ? rubyArray.entry(index) : UndefinedValue.UNDEFINED;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.OptArgMultipleAsgnInstr(this);
    }
}
