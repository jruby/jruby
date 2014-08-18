package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RegexpOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Represents a dynamic regexp in Ruby
// Ex: /#{a}#{b}/
public class BuildDynRegExpInstr extends Instr implements ResultInstr {
    private Variable result;
    private List<Operand> pieces;
    final private RegexpOptions options;

    // Cached regexp
    private RubyRegexp rubyRegexp;

    public BuildDynRegExpInstr(Variable result, List<Operand> pieces, RegexpOptions options) {
        super(Operation.BUILD_DREGEXP);

        this.options = options;
        this.pieces = pieces;
        this.result = result;
    }

    public List<Operand> getPieces() {
       return pieces;
    }

    public RegexpOptions getOptions() {
       return options;
    }

    public RubyRegexp getRegexp() {
       return rubyRegexp;
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Operand[] getOperands() {
        return pieces.toArray(new Operand[pieces.size()]);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        List<Operand> newPieces = new ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.getSimplifiedOperand(valueMap, force));
        }

       pieces = newPieces;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + java.util.Arrays.toString(pieces.toArray()) + "," + options + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        List<Operand> newPieces = new ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.cloneForInlining(ii));
        }

        return new BuildDynRegExpInstr(ii.getRenamedVariable(result), newPieces, options);
    }

    private RubyString[] retrievePieces(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        RubyString[] strings = new RubyString[pieces.size()];
        int i = 0;
        for (Operand p : pieces) {
            strings[i++] = (RubyString)p.retrieve(context, self, currScope, currDynScope, temp);
        }
        return strings;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // FIXME (from RegexpNode.java): 1.9 should care about internal or external encoding and not kcode.
        // If we have a constant regexp string or if the regexp patterns asks for caching, cache the regexp
        if ((rubyRegexp == null) || !options.isOnce() || context.runtime.getKCode() != rubyRegexp.getKCode()) {
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
