package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
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

public class LexicalSearchConstInstr extends Instr implements ResultInstr {
    Operand definingScope;
    String constName;
    private Variable result;

    // Constant caching
    private volatile transient Object cachedConstant = null;
    private Object generation = -1;
    private Invalidator invalidator;

    public LexicalSearchConstInstr(Variable result, Operand definingScope, String constName) {
        super(Operation.LEXICAL_SEARCH_CONST);

        assert result != null: "LexicalSearchConstInstr result is null";

        this.definingScope = definingScope;
        this.constName = constName;
        this.result = result;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { definingScope };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        definingScope = definingScope.getSimplifiedOperand(valueMap, force);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new LexicalSearchConstInstr(ii.getRenamedVariable(result), definingScope.cloneForInlining(ii), constName);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + definingScope + ", " + constName  + ")";
    }

    private Object cache(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Ruby runtime, Object constant) {
        StaticScope staticScope = (StaticScope) definingScope.retrieve(context, self, currDynScope, temp);
        RubyModule object = runtime.getObject();
        // SSS FIXME: IRManager objects dont have a static-scope yet, so this hack of looking up the module right away
        // This IR needs fixing!
        constant = (staticScope == null) ? object.getConstant(constName) : staticScope.getConstantInner(constName);
        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        } else {
            // recache
            generation = invalidator(runtime).getData();
            cachedConstant = constant;
        }
        return constant;
    }

    private boolean isCached(Ruby runtime, Object value) {
        return value != null && generation == invalidator(runtime).getData();
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        Object constant = cachedConstant; // Store to temp so it does null out on us mid-stream
        if (!isCached(runtime, constant)) constant = cache(context, currDynScope, self, temp, runtime, constant);

        return constant;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LexicalSearchConstInstr(this);
    }

    private Invalidator invalidator(Ruby runtime) {
        if (invalidator == null) {
            invalidator = runtime.getConstantInvalidator(constName);
        }
        return invalidator;
    }
}
