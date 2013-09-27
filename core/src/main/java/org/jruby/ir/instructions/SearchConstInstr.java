package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.Invalidator;

import java.util.Map;

// Const search:
// - looks up lexical scopes
// - then inheritance hierarcy if lexical search fails
// - then invokes const_missing if inheritance search fails
public class SearchConstInstr extends Instr implements ResultInstr {
    private Operand  startingScope;
    private String   constName;
    private boolean  noPrivateConsts;
    private Variable result;

    // Constant caching
    private volatile transient Object cachedConstant = null;
    private Object generation = -1;
    private Invalidator invalidator;

    public SearchConstInstr(Variable result, String constName, Operand startingScope, boolean noPrivateConsts) {
        super(Operation.SEARCH_CONST);

        assert result != null: "SearchConstInstr result is null";

        this.result          = result;
        this.constName       = constName;
        this.startingScope   = startingScope;
        this.noPrivateConsts = noPrivateConsts;
    }

    public Operand[] getOperands() {
        return new Operand[] { startingScope };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        startingScope = startingScope.getSimplifiedOperand(valueMap, force);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), constName, startingScope.cloneForInlining(ii), noPrivateConsts);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + constName + ", " + startingScope + ", no-private-consts=" + noPrivateConsts + ")";
    }

    public Object cache(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        StaticScope staticScope = (StaticScope) startingScope.retrieve(context, self, currDynScope, temp);
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
            constant = module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(constName));
        } else {
            // recache
            generation = runtime.getConstantInvalidator(constName).getData();
            cachedConstant = constant;
        }

        return constant;
    }

    public Object getCachedConst() {
        return cachedConstant;
    }

    public boolean isCached(ThreadContext context, Object value) {
        return value != null && generation == invalidator(context.getRuntime()).getData();
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object constant = cachedConstant; // Store to temp so it does null out on us mid-stream
        if (!isCached(context, constant)) constant = cache(context, currDynScope, self, temp);

        return constant;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SearchConstInstr(this);
    }

    public Operand getStartingScope() {
        return startingScope;
    }

    public String getConstName() {
        return constName;
    }

    public boolean isNoPrivateConsts() {
        return noPrivateConsts;
    }

    private Invalidator invalidator(Ruby runtime) {
        if (invalidator == null) {
            invalidator = runtime.getConstantInvalidator(constName);
        }
        return invalidator;
    }
}
