package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.ir.targets.simple.ConstantLookupSite;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

// Const search:
// - looks up lexical scopes
// - then inheritance hierarchy if lexical search fails
// - then invokes const_missing if inheritance search fails
public class SearchConstInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final RubySymbol constantName;
    private final boolean noPrivateConsts;

    // Constant caching
    private final ConstantLookupSite site;

    public SearchConstInstr(Variable result, RubySymbol constantName, Operand startingScope, boolean noPrivateConsts) {
        super(Operation.SEARCH_CONST, result, startingScope);

        assert result != null: "SearchConstInstr result is null";

        this.constantName = constantName;
        this.noPrivateConsts = noPrivateConsts;
        this.site = new ConstantLookupSite(constantName);
    }


    public Operand getStartingScope() {
        return getOperand1();
    }

    public String getId() {
        return constantName.idString();
    }

    public RubySymbol getName() {
        return constantName;
    }

    public boolean isNoPrivateConsts() {
        return noPrivateConsts;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), constantName, getStartingScope().cloneForInlining(ii), noPrivateConsts);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getName());
        e.encode(getStartingScope());
        e.encode(isNoPrivateConsts());
    }

    public static SearchConstInstr decode(IRReaderDecoder d) {
        return new SearchConstInstr(d.decodeVariable(), d.decodeSymbol(), d.decodeOperand(), d.decodeBoolean());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + getName(), "no_priv: " + noPrivateConsts};
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        StaticScope staticScope = (StaticScope) getStartingScope().retrieve(context, self, currScope, currDynScope, temp);
        return site.searchConst(context, staticScope, noPrivateConsts);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SearchConstInstr(this);
    }
}
