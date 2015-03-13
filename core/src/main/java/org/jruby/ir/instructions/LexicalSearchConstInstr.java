package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
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

public class LexicalSearchConstInstr extends ResultBaseInstr implements FixedArityInstr {
    String constName;

    // Constant caching
    private volatile transient ConstantCache cache;

    public LexicalSearchConstInstr(Variable result, Operand definingScope, String constName) {
        super(Operation.LEXICAL_SEARCH_CONST, result, new Operand[] { definingScope });

        assert result != null: "LexicalSearchConstInstr result is null";

        this.constName = constName;
    }

    public Operand getDefiningScope() {
        return operands[0];
    }

    public String getConstName() {
        return constName;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + constName };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new LexicalSearchConstInstr(ii.getRenamedVariable(result), getDefiningScope().cloneForInlining(ii), constName);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getDefiningScope());
        e.encode(getConstName());
    }

    private Object cache(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        StaticScope staticScope = (StaticScope) getDefiningScope().retrieve(context, self, currScope, currDynScope, temp);

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
