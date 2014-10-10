package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.*;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

import java.util.Map;

// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.

public class InheritanceSearchConstInstr extends Instr implements ResultInstr, FixedArityInstr {
    Operand  currentModule;
    String   constName;
    private Variable result;
    private final boolean  noPrivateConsts;

    // Constant caching
    private volatile transient ConstantCache cache;

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
        return new Operand[] { currentModule, new StringLiteral(constName), new UnboxedBoolean(noPrivateConsts) };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        currentModule = currentModule.getSimplifiedOperand(valueMap, force);
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
    public Instr clone(CloneInfo ii) {
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
            Invalidator invalidator = runtime.getConstantInvalidator(constName);
            cache = new ConstantCache((IRubyObject)constant, invalidator.getData(), invalidator, module.hashCode());
        }
        return constant;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Ruby runtime = context.runtime;
        Object cmVal = currentModule.retrieve(context, self, currScope, currDynScope, temp);
        RubyModule module;
        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw runtime.newTypeError(cmVal + " is not a type/class");
        }
        ConstantCache cache = this.cache;
        if (!ConstantCache.isCachedFrom(module, cache)) return cache(runtime, module);

        return cache.value;
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
}
