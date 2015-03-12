package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
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
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

import java.util.Map;

// Const search:
// - looks up lexical scopes
// - then inheritance hierarcy if lexical search fails
// - then invokes const_missing if inheritance search fails
public class SearchConstInstr extends ResultBaseInstr implements FixedArityInstr {
    private final String   constName;
    private final boolean  noPrivateConsts;

    // Constant caching
    private volatile transient ConstantCache cache;

    public SearchConstInstr(Variable result, String constName, Operand startingScope, boolean noPrivateConsts) {
        super(Operation.SEARCH_CONST, result, new Operand[] { startingScope });

        assert result != null: "SearchConstInstr result is null";

        this.constName       = constName;
        this.noPrivateConsts = noPrivateConsts;
    }


    public Operand getStartingScope() {
        return operands[0];
    }

    public String getConstName() {
        return constName;
    }

    public boolean isNoPrivateConsts() {
        return noPrivateConsts;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), constName, getStartingScope().cloneForInlining(ii), noPrivateConsts);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getConstName());
        e.encode(getStartingScope());
        e.encode(isNoPrivateConsts());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + constName, "no_priv: " + noPrivateConsts};
    }

    public ConstantCache getConstantCache() {
        return cache;
    }

    public Object cache(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        StaticScope staticScope = (StaticScope) getStartingScope().retrieve(context, self, currScope, currDynScope, temp);
        Object constant = (staticScope == null) ? object.getConstant(constName) : staticScope.getConstantInner(constName);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);
        }

        // Call const_missing or cache
        if (constant == null) {
            constant = module.callMethod(context, "const_missing", runtime.fastNewSymbol(constName));
        } else {
            // recache
            Invalidator invalidator = runtime.getConstantInvalidator(constName);
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
