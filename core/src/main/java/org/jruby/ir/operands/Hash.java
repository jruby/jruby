package org.jruby.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.KeyValuePair;

import java.util.List;
import java.util.Iterator;
import java.util.Map;

// Represents a hash { _ =>_, _ => _ .. } in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this hash could get converted to calls
// that actually build the hash
public class Hash extends Operand {
    final public List<KeyValuePair<Operand, Operand>> pairs;

    // Is this a hash used to represent a keyword hash to be setup for ZSuper?
    // SSS FIXME: Quick hack for now - this should probably be done with an overloaded operand.
    final public boolean isKWArgsHash;

    public Hash(List<KeyValuePair<Operand, Operand>> pairs, boolean isKWArgsHash) {
        super(OperandType.HASH);

        this.pairs = pairs;
        this.isKWArgsHash = isKWArgsHash;
    }

    public Hash(List<KeyValuePair<Operand, Operand>> pairs) {
        this(pairs, false);
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

        return new Hash(newPairs, isKWArgsHash);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        for (KeyValuePair<Operand, Operand> pair : pairs) {
            pair.getKey().addUsedVariables(l);
            pair.getValue().addUsedVariables(l);
        }
    }

    public Operand cloneForLVarDepth(int newDepth) {
        List<KeyValuePair<Operand, Operand>> newPairs = new java.util.ArrayList<KeyValuePair<Operand, Operand>>();
        for (KeyValuePair<Operand, Operand> pair : pairs) {
            newPairs.add(new KeyValuePair(pair.getKey(), ((DepthCloneable) pair.getValue()).cloneForDepth(newDepth)));
        }
        return new Hash(newPairs, isKWArgsHash);
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
        return new Hash(newPairs, isKWArgsHash);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        Ruby runtime = context.runtime;
        RubyHash hash;
        Iterator<KeyValuePair<Operand, Operand>> it = pairs.iterator();

        if (isKWArgsHash && pairs.get(0).getKey() == Symbol.KW_REST_ARG_DUMMY) {
            // Dup the rest args hash and use that as the basis for inserting the non-rest args
            hash = ((RubyHash) pairs.get(0).getValue().retrieve(context, self, currScope, currDynScope, temp)).dupFast(context);
            // Skip the first pair
            it.next();
        } else {
            hash = RubyHash.newHash(runtime);
        }

        while (it.hasNext()) {
            KeyValuePair<Operand, Operand> pair = (KeyValuePair<Operand, Operand>) it.next();
            IRubyObject key = (IRubyObject) pair.getKey().retrieve(context, self, currScope, currDynScope, temp);
            IRubyObject value = (IRubyObject) pair.getValue().retrieve(context, self, currScope, currDynScope, temp);

            hash.fastASetCheckString(runtime, key, value);
        }

        return hash;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Hash(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(pairs.size());
        for (KeyValuePair<Operand, Operand> pair: pairs) {
            e.encode(pair.getKey());
            e.encode(pair.getValue());
        }
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
