package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.Invalidator;

import java.util.Map;

// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.

public class InheritanceSearchConstInstr extends Instr implements ResultInstr {
    Operand  currentModule;
    String   constName;
    private Variable result;
    private boolean  noPrivateConsts;

    // Constant caching
    private volatile transient Object cachedConstant = null;
    private volatile int hash = -1;
    private volatile Object generation = -1;
    private Invalidator invalidator;

    public InheritanceSearchConstInstr(Variable result, Operand currentModule, String constName, boolean noPrivateConsts) {
        super(Operation.INHERITANCE_SEARCH_CONST);

        assert result != null: "InheritanceSearchConstInstr result is null";

        this.currentModule = currentModule;
        this.constName = constName;
        this.result = result;
        this.noPrivateConsts = noPrivateConsts;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { currentModule };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        currentModule = currentModule.getSimplifiedOperand(valueMap, force);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new InheritanceSearchConstInstr(ii.getRenamedVariable(result), currentModule.cloneForInlining(ii), constName, noPrivateConsts);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + currentModule + ", " + constName  + ")";
    }

    private Object cache(Ruby runtime, RubyModule module) {
        Object constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);
        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        } else {
            // recache
            generation = runtime.getConstantInvalidator(constName).getData();
            hash = module.hashCode();
            cachedConstant = constant;
        }
        return constant;
    }

    private boolean isCached(Ruby runtime, RubyModule target, Object value) {
        return value != null && generation == invalidator(runtime).getData() && hash == target.hashCode();
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        Object cmVal = currentModule.retrieve(context, self, currDynScope, temp);
        RubyModule module;
        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw runtime.newTypeError(cmVal + " is not a type/class");
        }
        Object constant = cachedConstant; // Store to temp so it does null out on us mid-stream
        if (!isCached(runtime, module, constant)) constant = cache(runtime, module);

        return constant;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InheritanceSearchConstInstr(this);
    }

    public Operand getCurrentModule() {
        return currentModule;
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
