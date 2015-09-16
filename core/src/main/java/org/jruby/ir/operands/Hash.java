package org.jruby.ir.operands;

import java.util.ArrayList;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
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
    final public HashPair[] pairs;

    // Is this a hash used to represent a keyword hash to be setup for ZSuper?
    // SSS FIXME: Quick hack for now - this should probably be done with an overloaded operand.
    final public boolean isKWArgsHash;

    public Hash(List<KeyValuePair<Operand, Operand>> pairs) {
        this(pairs, false);
    }

    public Hash(List<KeyValuePair<Operand, Operand>> pairs, boolean isKWArgsHash) {
        HashPair[] newPairs = new HashPair[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            newPairs[i] = new HashPair(pairs.get(i).getKey(), pairs.get(i).getValue());
        }

        this.pairs = newPairs;
        this.isKWArgsHash = isKWArgsHash;
    }

    public Hash(HashPair[] pairs, boolean isKWArgsHash) {
        super();

        this.pairs = pairs;
        this.isKWArgsHash = isKWArgsHash;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.HASH;
    }

    public boolean isBlank() {
        return pairs == null || pairs.length == 0;
    }

    @Override
    public boolean hasKnownValue() {
        for (HashPair pair : pairs) {
            if (!pair.getKey().hasKnownValue() || !pair.getValue().hasKnownValue())
                return false;
        }

        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        HashPair[] newPairs = new HashPair[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            HashPair pair = pairs[i];
            newPairs[i] =
                    new HashPair(
                            pair.getKey().getSimplifiedOperand(valueMap, force),
                            pair.getValue().getSimplifiedOperand(valueMap, force));
        }

        return new Hash(newPairs, isKWArgsHash);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        for (HashPair pair : pairs) {
            pair.getKey().addUsedVariables(l);
            pair.getValue().addUsedVariables(l);
        }
    }

    public Operand cloneForLVarDepth(int newDepth) {
        HashPair[] newPairs = new HashPair[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            HashPair pair = pairs[i];
            newPairs[i] = new HashPair(pair.getKey(), ((DepthCloneable) pair.getValue()).cloneForDepth(newDepth));
        }
        return new Hash(newPairs, isKWArgsHash);
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        if (hasKnownValue())
            return this;

        HashPair[] newPairs = new HashPair[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            HashPair pair = pairs[i];
            newPairs[i] = new HashPair(pair.getKey().cloneForInlining(ii), pair.getValue().cloneForInlining(ii));
        }
        return new Hash(newPairs, isKWArgsHash);
    }

    // TODO: For small literal hashes, we should use RubyHash.newSmallHash plus appropriate inserts to avoid unnecessary buckets
    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        Ruby runtime = context.runtime;
        RubyHash hash;
        int i = 0;

        if (isKWArgsHash && pairs.length > 0 && pairs[0].getKey() == Symbol.KW_REST_ARG_DUMMY) {
            // Dup the rest args hash and use that as the basis for inserting the non-rest args
            hash = ((RubyHash) pairs[0].getValue().retrieve(context, self, currScope, currDynScope, temp)).dupFast(context);
            // Skip the first pair
            i = 1;
        } else {
            hash = RubyHash.newHash(runtime);
        }

        for (; i < pairs.length; i++) {
            HashPair pair = pairs[i];
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
        e.encode(pairs.length);
        for (HashPair pair: pairs) {
            e.encode(pair.getKey());
            e.encode(pair.getValue());
        }
        e.encode(isKWArgsHash);
    }

    public static Hash decode(IRReaderDecoder d) {
        int size = d.decodeInt();
        HashPair[] newPairs = new HashPair[size];
        for (int i = 0; i < size; i++) {
            newPairs[i] = new HashPair(d.decodeOperand(), d.decodeOperand());
        }

        return new Hash(newPairs, d.decodeBoolean());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isKWArgsHash) builder.append("KW");
        builder.append("{");
        if (!isBlank()) {
            int pairCount = pairs.length;
            for (int i = 0; i < pairCount; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(pairs[i]);
            }
        }
        builder.append("}");
        return builder.toString();
    }

    public HashPair[] getPairs() {
        return pairs;
    }
}
