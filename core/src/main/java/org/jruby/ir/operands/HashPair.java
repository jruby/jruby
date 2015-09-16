package org.jruby.ir.operands;

import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.KeyValuePair;

import java.util.List;
import java.util.Map;

/**
 * Represents a key/value pair in a literal hash (currently only used for hash/keyword args).
 */
public class HashPair extends Operand implements Map.Entry<Operand, Operand> {
    public final Operand key;
    public final Operand value;

    public HashPair(Operand key, Operand value) {
        super();

        this.key = key;
        this.value = value;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.HASH_PAIR;
    }

    @Override
    public boolean hasKnownValue() {
        if (!getKey().hasKnownValue() || !getValue().hasKnownValue())
            return false;

        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        return new HashPair(
                        getKey().getSimplifiedOperand(valueMap, force),
                        getValue().getSimplifiedOperand(valueMap, force));
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        getKey().addUsedVariables(l);
        getValue().addUsedVariables(l);
    }

    public Operand cloneForLVarDepth(int newDepth) {
        return new HashPair(
                getKey(),
                ((DepthCloneable) getValue()).cloneForDepth(newDepth));
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        if (hasKnownValue())
            return this;

        return new HashPair(
                getKey().cloneForInlining(ii),
                getValue().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getKey());
        e.encode(getValue());
    }

    public static HashPair decode(IRReaderDecoder d) {
        return new HashPair(d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public String toString() {
        return getKey() + "=>" + getValue();
    }

    @Override
    public Operand getKey() {
        return key;
    }

    @Override
    public Operand getValue() {
        return value;
    }

    @Override
    public Operand setValue(Operand value) {
        throw new UnsupportedOperationException();
    }
}
