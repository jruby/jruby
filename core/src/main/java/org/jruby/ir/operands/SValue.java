package org.jruby.ir.operands;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

// Represents a svalue node in Ruby code
//
// According to headius, svalue evaluates its value node and returns:
//  * nil if it does not evaluate to an array or if it evaluates to an empty array
//  * the first element if it evaluates to a one-element array
//  * the array if it evaluates to a >1 element array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class SValue extends Operand {
    final private Operand array;

    public SValue(Operand array) {
        super(OperandType.SVALUE);

        this.array = array;
    }

    public Operand getArray() {
        return array;
    }

    @Override
    public boolean hasKnownValue() {
        return array.hasKnownValue();
    }

    @Override
    public String toString() {
        return "SValue:" + array;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newArray = array.getSimplifiedOperand(valueMap, force);
        if (newArray instanceof Array) {
            Array a = (Array) newArray;
            return (a.getElts().length == 1) ? a.getElts()[0] : a;
        } else {
            return (newArray == array) ? this : new SValue(newArray);
        }
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        array.addUsedVariables(l);
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return hasKnownValue() ? this : new SValue(array.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        Object val = array.retrieve(context, self, currScope, currDynScope, temp);

        return (val instanceof RubyArray) ? val : context.runtime.getNil();
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArray());
    }

    public static SValue decode(IRReaderDecoder d) {
        return new SValue(d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SValue(this);
    }
}
