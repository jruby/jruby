package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyHash;

import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a hash { _ =>_, _ => _ .. } in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this hash could get converted to calls
// that actually build the hash
public class Hash extends Operand {
    final public List<KeyValuePair> pairs;

    public Hash(List<KeyValuePair> pairs) {
        this.pairs = pairs;
    }

    public boolean isBlank() {
        return pairs == null || pairs.isEmpty();
    }

    @Override
    public boolean isConstant() {
        for (KeyValuePair pair : pairs) {
            if (!pair.getKey().isConstant() || !pair.getValue().isConstant()) return false;
        }

        return true;
    }

    @Override
    public boolean isNonAtomicValue() {
        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        int i = 0;
        for (KeyValuePair pair : pairs) {
            pair.setKey(pair.getKey().getSimplifiedOperand(valueMap));
            pair.setValue(pair.getValue().getSimplifiedOperand(valueMap));
            i++;
        }

        return this;
    }

    @Override
    public IRClass getTargetClass() {
        return IRClass.getCoreClass("Hash");
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        for (KeyValuePair pair : pairs) {
            pair.getKey().addUsedVariables(l);
            pair.getValue().addUsedVariables(l);
        }
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        if (isConstant()) return this;

        List<KeyValuePair> newPairs = new java.util.ArrayList<KeyValuePair>();
        for (KeyValuePair pair : pairs) {
            newPairs.add(new KeyValuePair(pair.getKey().cloneForInlining(ii),
                    pair.getValue().cloneForInlining(ii)));
        }
        return new Hash(newPairs);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        Ruby runtime = interp.getRuntime();
        RubyHash hash = RubyHash.newHash(runtime);

        for (KeyValuePair pair : pairs) {
            hash.fastASetCheckString(runtime, (IRubyObject) pair.getKey().retrieve(interp),
                    (IRubyObject) pair.getValue().retrieve(interp));
        }

        return hash;
    }
}
