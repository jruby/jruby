package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
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

public class LexicalSearchConstInstr extends Instr implements ResultInstr, FixedArityInstr {
    Operand definingScope;
    String constName;
    private Variable result;

    // Constant caching
    private volatile transient ConstantCache cache;

    public LexicalSearchConstInstr(Variable result, Operand definingScope, String constName) {
        super(Operation.LEXICAL_SEARCH_CONST);

        assert result != null: "LexicalSearchConstInstr result is null";

        this.definingScope = definingScope;
        this.constName = constName;
        this.result = result;
    }

    public Operand getDefiningScope() {
        return definingScope;
    }

    public String getConstName() {
        return constName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { definingScope, new StringLiteral(constName) };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        definingScope = definingScope.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + definingScope + ", " + constName  + ")";
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new LexicalSearchConstInstr(ii.getRenamedVariable(result), definingScope.cloneForInlining(ii), constName);
    }

    private Object cache(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        StaticScope staticScope = (StaticScope) definingScope.retrieve(context, self, currScope, currDynScope, temp);

        // CON FIXME: Removed SSS hack for IRManager objects not having a static scope, so we can find and fix

        IRubyObject constant = staticScope.getConstantInner(constName);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        } else {
            // recache
            Invalidator invalidator = context.runtime.getConstantInvalidator(constName);
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
