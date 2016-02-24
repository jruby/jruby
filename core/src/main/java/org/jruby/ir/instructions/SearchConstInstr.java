package org.jruby.ir.instructions;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Symbol;
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

// Const search:
// - looks up lexical scopes
// - then inheritance hierarcy if lexical search fails
// - then invokes const_missing if inheritance search fails
public class SearchConstInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final Symbol symbol;
    private final boolean  noPrivateConsts;

    // Constant caching
    private volatile transient ConstantCache cache;

    public SearchConstInstr(Variable result, Symbol symbol, Operand startingScope, boolean noPrivateConsts) {
        super(Operation.SEARCH_CONST, result, startingScope);

        assert result != null: "SearchConstInstr result is null";

        this.symbol              = symbol;
        this.noPrivateConsts = noPrivateConsts;
    }


    public Operand getStartingScope() {
        return getOperand1();
    }

    public Symbol getConstName() {
        return symbol;
    }

    public boolean isNoPrivateConsts() {
        return noPrivateConsts;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), symbol, getStartingScope().cloneForInlining(ii), noPrivateConsts);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(symbol);
        e.encode(getStartingScope());
        e.encode(isNoPrivateConsts());
    }

    public static SearchConstInstr decode(IRReaderDecoder d) {
        return new SearchConstInstr(d.decodeVariable(), (Symbol) d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + symbol.getName(), "no_priv: " + noPrivateConsts};
    }

    public ConstantCache getConstantCache() {
        return cache;
    }

    public Object cache(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        StaticScope staticScope = (StaticScope) getStartingScope().retrieve(context, self, currScope, currDynScope, temp);
        RubySymbol symName = (RubySymbol) symbol.retrieve(context, self, currScope, currDynScope, temp);
        String rawName = symName.toID();
        Object constant = (staticScope == null) ? object.getConstant(rawName) : staticScope.getConstantInner(rawName);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = noPrivateConsts ? module.getConstantFromNoConstMissing(rawName, false) : module.getConstantNoConstMissing(rawName);
        }

        // Call const_missing or cache
        if (constant == null) {
            constant = module.callMethod(context, "const_missing", symName);
        } else {
            // recache
            Invalidator invalidator = runtime.getConstantInvalidator(rawName);
            cache = new ConstantCache((IRubyObject)constant, invalidator.getData(), invalidator);
        }

        return constant;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        ConstantCache cache = this.cache;
        if (!ConstantCache.isCached(cache)) return cache(context, currScope, currDynScope, self, temp);

        return cache.value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SearchConstInstr(this);
    }
}
