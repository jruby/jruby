package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.

public class LexicalSearchConstInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final Symbol symbol;

    // Constant caching
    private volatile transient ConstantCache cache;

    public LexicalSearchConstInstr(Variable result, Operand definingScope, Symbol symbol) {
        super(Operation.LEXICAL_SEARCH_CONST, result, definingScope);

        assert result != null: "LexicalSearchConstInstr result is null";

        this.symbol = symbol;
    }

    public Operand getDefiningScope() {
        return getOperand1();
    }

    public Symbol getConstName() {
        return symbol;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + symbol.getName() };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new LexicalSearchConstInstr(ii.getRenamedVariable(result), getDefiningScope().cloneForInlining(ii), symbol);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getDefiningScope());
        e.encode(symbol);
    }

    public static LexicalSearchConstInstr decode(IRReaderDecoder d) {
        return new LexicalSearchConstInstr(d.decodeVariable(), d.decodeOperand(), (Symbol) d.decodeOperand());
    }

    private Object cache(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        StaticScope staticScope = (StaticScope) getDefiningScope().retrieve(context, self, currScope, currDynScope, temp);
        RubySymbol symName = (RubySymbol) symbol.retrieve(context, self, currScope, currDynScope, temp);

        IRubyObject constant = staticScope.getConstantInner(symName.toID());

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        } else {
            // recache
            Invalidator invalidator = context.runtime.getConstantInvalidator(symName.toID());
            cache = new ConstantCache(constant, invalidator.getData(), invalidator);
        }

        return constant;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        ConstantCache cache = this.cache; // Store to temp so it does null out on us mid-stream
        if (!ConstantCache.isCached(cache)) return cache(context, currScope, currDynScope, self, temp);

        return cache.value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LexicalSearchConstInstr(this);
    }
}
