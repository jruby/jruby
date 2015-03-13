package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RegexpOptions;

// Represents a dynamic regexp in Ruby
// Ex: /#{a}#{b}/
public class BuildDynRegExpInstr extends ResultBaseInstr {
    final private RegexpOptions options;

    // Cached regexp
    private RubyRegexp rubyRegexp;

    public BuildDynRegExpInstr(Variable result, Operand[] pieces, RegexpOptions options) {
        super(Operation.BUILD_DREGEXP, result, pieces);

        this.options = options;
    }

    public Operand[] getPieces() {
       return operands;
    }

    public RegexpOptions getOptions() {
       return options;
    }

    public RubyRegexp getRegexp() {
       return rubyRegexp;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"options: " + options};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildDynRegExpInstr(ii.getRenamedVariable(result), cloneOperands(ii), options);
    }

    private RubyString[] retrievePieces(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        int length = operands.length;
        RubyString[] strings = new RubyString[length];
        for (int i = 0; i < length; i++) {
            strings[i] = (RubyString) operands[i].retrieve(context, self, currScope, currDynScope, temp);
        }
        return strings;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getPieces());
        e.encode(getOptions().toEmbeddedOptions());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // FIXME (from RegexpNode.java): 1.9 should care about internal or external encoding and not kcode.
        // If we have a constant regexp string or if the regexp patterns asks for caching, cache the regexp
        if (rubyRegexp == null || !options.isOnce() || context.runtime.getKCode() != rubyRegexp.getKCode()) {
            RubyString[] pieces  = retrievePieces(context, self, currScope, currDynScope, temp);
            RubyString   pattern = RubyRegexp.preprocessDRegexp(context.runtime, pieces, options);
            RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
            re.setLiteral();
            rubyRegexp = re;
        }

        return rubyRegexp;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildDynRegExpInstr(this);
    }
}
