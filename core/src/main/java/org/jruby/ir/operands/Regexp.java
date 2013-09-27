package org.jruby.ir.operands;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RegexpOptions;

import java.util.List;
import java.util.Map;

// Represents a regexp from ruby
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this regexp operand could get converted to calls
// that actually build the Regexp object
public class Regexp extends Operand {
    final public RegexpOptions options;
    final private Operand regexp;
    private RubyRegexp rubyRegexp;

    public Regexp(Operand regexp, RegexpOptions options) {
        this.regexp = regexp;
        this.options = options;
    }

    @Override
    public boolean hasKnownValue() {
        return regexp.hasKnownValue();
    }

    @Override
    public String toString() {
        return "RE:|" + regexp + "|" + options;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newRegexp = regexp.getSimplifiedOperand(valueMap, force);
        return newRegexp == regexp ? this : new Regexp(newRegexp, options);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        regexp.addUsedVariables(l);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return hasKnownValue() ? this : new Regexp(regexp.cloneForInlining(ii), options);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        // FIXME (from RegexpNode.java): 1.9 should care about internal or external encoding and not kcode.
        // If we have a constant regexp string or if the regexp patterns asks for caching, cache the regexp
        if ((!regexp.hasKnownValue() && !options.isOnce()) || (rubyRegexp == null) || context.runtime.getKCode() != rubyRegexp.getKCode()) {
            RubyRegexp re;
            if (regexp instanceof CompoundString) {
                if (context.runtime.is1_9()) {
                    RubyString[] pieces  = ((CompoundString)regexp).retrievePieces(context, self, currDynScope, temp);
                    RubyString   pattern = RubyRegexp.preprocessDRegexp(context.runtime, pieces, options);
                    re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
                } else {
                    RubyString pattern = (RubyString) regexp.retrieve(context, self, currDynScope, temp);
                    re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
                }
            } else {
                RubyString pattern = (RubyString) regexp.retrieve(context, self, currDynScope, temp);
                re = RubyRegexp.newRegexp(context.runtime, pattern.getByteList(), options);
            }
            re.setLiteral();
            rubyRegexp = re;
        }

        return rubyRegexp;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Regexp(this);
    }
}
