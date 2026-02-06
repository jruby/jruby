package org.jruby.ir.operands;

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
import org.jruby.util.TypeConverter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Create.newHash;
import static org.jruby.api.Create.newSmallHash;

// Represents a hash { _ =>_, _ => _ .. } in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this hash could get converted to calls
// that actually build the hash
public class Hash extends Operand {
    public final KeyValuePair<Operand, Operand>[] pairs;
    public final boolean keywords;

    public Hash(List<KeyValuePair<Operand, Operand>> pairs) {
        this(pairs, false);
    }

    public Hash(List<KeyValuePair<Operand, Operand>> pairs, boolean keywords) {
        this(pairs.toArray(new KeyValuePair[pairs.size()]), keywords);
    }

    protected Hash(KeyValuePair<Operand, Operand>[] pairs) {
        this(pairs, false);
    }

    protected Hash(KeyValuePair<Operand, Operand>[] pairs, boolean keywords) {
        super();

        this.pairs = pairs;
        this.keywords = keywords;
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
        for (KeyValuePair<Operand, Operand> pair : pairs) {
            if (!pair.getKey().hasKnownValue() || !pair.getValue().hasKnownValue())
                return false;
        }

        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        KeyValuePair<Operand, Operand>[] newPairs = Arrays.stream(pairs)
                .map(pair ->
                        new KeyValuePair<>(
                                pair.getKey().getSimplifiedOperand(valueMap, force),
                                pair.getValue().getSimplifiedOperand(valueMap, force)))
                .toArray(n -> new KeyValuePair[n]);

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

    public Operand cloneForLVarDepth(int newDepth) {
        KeyValuePair<Operand, Operand>[] newPairs = Arrays.stream(pairs)
                .map(pair ->
                        new KeyValuePair(
                                pair.getKey(),
                                ((DepthCloneable) pair.getValue()).cloneForDepth(newDepth)))
                .toArray(n -> new KeyValuePair[n]);

        return new Hash(newPairs);
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        if (hasKnownValue())
            return this;

        KeyValuePair<Operand, Operand>[] newPairs = Arrays.stream(pairs)
                .map(pair ->
                        new KeyValuePair(
                                pair.getKey().cloneForInlining(ii),
                                pair.getValue().cloneForInlining(ii)))
                .toArray(n -> new KeyValuePair[n]);

        return new Hash(newPairs);
    }

    public boolean isKeywordRest() {
        return pairs.length > 0 && pairs[0].getKey().equals(Symbol.KW_REST_ARG_DUMMY);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        Ruby runtime = context.runtime;
        RubyHash hash;
        KeyValuePair<Operand, Operand>[] pairs = this.pairs;
        int index = 0;
        int length = pairs.length / 2;
        boolean smallHash = length <= 10;

        if (isKeywordRest()) {
            // Dup the rest args hash and use that as the basis for inserting the non-rest args
            IRubyObject rest = (IRubyObject) pairs[0].getValue().retrieve(context, self, currScope, currDynScope, temp);
            TypeConverter.checkType(context, rest, hashClass(context));
            hash = ((RubyHash) rest).dupFast(context);
            // Skip the first pair
            index++;
        } else {
            hash = smallHash ? newSmallHash(context) : newHash(context);
        }


        for (int i = index; i < pairs.length; i++) {
            KeyValuePair<Operand, Operand> pair = pairs[i];
            IRubyObject key = (IRubyObject) pair.getKey().retrieve(context, self, currScope, currDynScope, temp);
            IRubyObject value = (IRubyObject) pair.getValue().retrieve(context, self, currScope, currDynScope, temp);

            if (smallHash) {
                hash.fastASetSmallCheckString(context.runtime, key, value);
            } else {
                hash.fastASetCheckString(context.runtime, key, value);
            }
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
        for (KeyValuePair<Operand, Operand> pair: pairs) {
            e.encode(pair.getKey());
            e.encode(pair.getValue());
        }
    }

    public static Hash decode(IRReaderDecoder d) {
        int size = d.decodeInt();
        KeyValuePair<Operand, Operand> pairs[] = new KeyValuePair[size];

        for (int i = 0; i < size; i++) {
            pairs[i] = new KeyValuePair(d.decodeOperand(), d.decodeOperand());
        }

        return new Hash(pairs);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
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

    public List<KeyValuePair<Operand, Operand>> getPairs() {
        return Arrays.asList(pairs);
    }
}
