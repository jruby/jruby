package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.targets.simple.ConstantLookupSite;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

/**
 * Search for a constant within the current module.  If it cannot find then
 * call const_missing.
 */
public class SearchModuleForConstInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final RubySymbol constantName;
    private final boolean noPrivateConsts;
    private final boolean callConstMissing;

    // Constant caching
    private final ConstantLookupSite site;

    public SearchModuleForConstInstr(Variable result, Operand currentModule, RubySymbol constantName, boolean noPrivateConsts) {
        this(result, currentModule, constantName, noPrivateConsts, true);
    }

    public SearchModuleForConstInstr(Variable result, Operand currentModule, RubySymbol constantName,
                                     boolean noPrivateConsts, boolean callConstMissing) {
        super(Operation.SEARCH_MODULE_FOR_CONST, result, currentModule);

        this.constantName = constantName;
        this.noPrivateConsts = noPrivateConsts;
        this.callConstMissing = callConstMissing;
        this.site = new ConstantLookupSite(constantName);
    }

    public Operand getCurrentModule() {
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

    public boolean callConstMissing() {
        return callConstMissing;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new SearchModuleForConstInstr(ii.getRenamedVariable(result),
                getCurrentModule().cloneForInlining(ii), constantName, noPrivateConsts, callConstMissing);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + constantName, "no_priv: " + noPrivateConsts};
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getCurrentModule());
        e.encode(getName());
        e.encode(isNoPrivateConsts());
        e.encode(callConstMissing());
    }

    public static SearchModuleForConstInstr decode(IRReaderDecoder d) {
        return new SearchModuleForConstInstr(d.decodeVariable(), d.decodeOperand(), d.decodeSymbol(),
                d.decodeBoolean(), d.decodeBoolean());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object cmVal = getCurrentModule().retrieve(context, self, currScope, currDynScope, temp);

        return site.searchModuleForConst(context, (IRubyObject) cmVal, noPrivateConsts, callConstMissing);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SearchModuleForConstInstr(this);
    }
}
