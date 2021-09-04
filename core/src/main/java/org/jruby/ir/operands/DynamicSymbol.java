package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jcodings.Encoding;

import java.util.List;
import java.util.Map;

public class DynamicSymbol extends Operand {
    final private Operand symbolName;

    public DynamicSymbol(Operand n) {
        super();

        symbolName = n;
   }

    @Override
    public OperandType getOperandType() {
        return OperandType.DYNAMIC_SYMBOL;
    }

    public Operand getSymbolName() {
        return symbolName;
    }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newSymbol = symbolName.getSimplifiedOperand(valueMap, force);
        return symbolName == newSymbol ? this : new DynamicSymbol(newSymbol);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        symbolName.addUsedVariables(l);
    }

    public Operand cloneForInlining(CloneInfo ii) {
        Operand clonedSymbolName = symbolName.cloneForInlining(ii);

        return clonedSymbolName == symbolName ? this : new DynamicSymbol(clonedSymbolName);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        IRubyObject obj = (IRubyObject) symbolName.retrieve(context, self, currScope, currDynScope, temp);
        return IRRuntimeHelpers.newDSymbol(context, obj);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DynamicSymbol(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(symbolName);
    }

    public static DynamicSymbol decode(IRReaderDecoder d) {
        return new DynamicSymbol(d.decodeOperand());
    }

    @Override
    public String toString() {
        return ":" + symbolName.toString();
    }
}
