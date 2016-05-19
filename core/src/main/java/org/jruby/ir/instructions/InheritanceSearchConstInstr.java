package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.*;
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

public class InheritanceSearchConstInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    String   constName;

    // Constant caching
    private volatile transient ConstantCache cache;

    public InheritanceSearchConstInstr(Variable result, Operand currentModule, String constName) {
        super(Operation.INHERITANCE_SEARCH_CONST, result, currentModule);

        assert result != null: "InheritanceSearchConstInstr result is null";

        this.constName = constName;
    }

    @Deprecated
    public InheritanceSearchConstInstr(Variable result, Operand currentModule, String constName, boolean unused) {
        this(result, currentModule, constName);
    }

    public Operand getCurrentModule() {
        return getOperand1();
    }

    public String getConstName() {
        return constName;
    }

    @Deprecated
    public boolean isNoPrivateConsts() {
        return false;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new InheritanceSearchConstInstr(ii.getRenamedVariable(result), getCurrentModule().cloneForInlining(ii), constName);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + constName };
    }

    private Object cache(Ruby runtime, RubyModule module) {
        Object constant = module.getConstantNoConstMissingSKipAutoload(constName);
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
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getCurrentModule());
        e.encode(getConstName());
    }

    public static InheritanceSearchConstInstr decode(IRReaderDecoder d) {
        return new InheritanceSearchConstInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object cmVal = getCurrentModule().retrieve(context, self, currScope, currDynScope, temp);

        if (!(cmVal instanceof RubyModule)) throw context.runtime.newTypeError(cmVal + " is not a type/class");

        RubyModule module = (RubyModule) cmVal;
        ConstantCache cache = this.cache;

        return !ConstantCache.isCachedFrom(module, cache) ? cache(context.runtime, module) : cache.value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InheritanceSearchConstInstr(this);
    }
}
