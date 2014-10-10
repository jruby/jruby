package org.jruby.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.KeyValuePair;

import java.util.List;
import java.util.Map;

// Represents a hash { _ =>_, _ => _ .. } in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this hash could get converted to calls
// that actually build the hash
public class Hash extends Operand {
    final public List<KeyValuePair<Operand, Operand>> pairs;

    public Hash(List<KeyValuePair<Operand, Operand>> pairs) {
        super(OperandType.HASH);

        this.pairs = pairs;
    }

    public boolean isBlank() {
        return pairs == null || pairs.isEmpty();
    }

    @Override
    public boolean hasKnownValue() {
        for (KeyValuePair<Operand, Operand> pair : pairs) {
            if (!pair.getKey().hasKnownValue() || !pair.getValue().hasKnownValue())
                return false;
        }

        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        List<KeyValuePair<Operand, Operand>> newPairs = new java.util.ArrayList<KeyValuePair<Operand, Operand>>();
        for (KeyValuePair<Operand, Operand> pair : pairs) {
            newPairs.add(new KeyValuePair(pair.getKey().getSimplifiedOperand(valueMap, force), pair
                    .getValue().getSimplifiedOperand(valueMap, force)));
        }

        return new Hash(newPairs);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        for (KeyValuePair<Operand, Operand> pair : pairs) {
            pair.getKey().addUsedVariables(l);
            pair.getValue().addUsedVariables(l);
        }
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        if (hasKnownValue())
            return this;

        List<KeyValuePair<Operand, Operand>> newPairs = new java.util.ArrayList<KeyValuePair<Operand, Operand>>();
        for (KeyValuePair<Operand, Operand> pair : pairs) {
            newPairs.add(new KeyValuePair(pair.getKey().cloneForInlining(ii), pair.getValue()
                    .cloneForInlining(ii)));
        }
        return new Hash(newPairs);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope,
                           Object[] temp) {
        Ruby runtime = context.runtime;
        RubyHash hash = RubyHash.newHash(runtime);

        for (KeyValuePair<Operand, Operand> pair : pairs) {
            IRubyObject key = (IRubyObject) pair.getKey().retrieve(context, self, currScope, currDynScope,
                    temp);
            IRubyObject value = (IRubyObject) pair.getValue().retrieve(context, self, currScope, currDynScope,
                    temp);

            hash.fastASetCheckString(runtime, key, value);
        }

        return hash;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Hash(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        if (!isBlank()) {
            int pairCount = pairs.size();
            for (int i = 0; i < pairCount; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(pairs.get(i));
            }
        }
        builder.append("}");
        return builder.toString();
    }

    public List<KeyValuePair<Operand, Operand>> getPairs() {
        return pairs;
    }
}
