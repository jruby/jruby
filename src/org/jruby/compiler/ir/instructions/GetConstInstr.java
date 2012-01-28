package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A constant which we is not lexically scoped.  This instruction will ask
 * the source operand to get it directly.
 */
public class GetConstInstr extends GetInstr {
    // Fields required for constant caching
    private volatile transient Object cachedConstant = null;
    private volatile Object generation = -1;
    private volatile int hash = -1;

    public GetConstInstr(Variable dest, Operand scopeOrObj, String constName) {
        super(Operation.GET_CONST, dest, scopeOrObj, constName);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetConstInstr(ii.getRenamedVariable(getResult()), getSource().cloneForInlining(ii), getRef());
    }

    private boolean isCached(Ruby runtime, RubyModule target, Object value) {
        // We could probably also detect if LHS value came out of cache and avoid some of this
        return value != null &&
               generation == runtime.getConstantInvalidator().getData() &&
               hash == target.hashCode();
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.getRuntime();
        Object source = getSource().retrieve(context, self, currDynScope, temp);
        RubyModule module;

        if (source instanceof RubyModule) {
            module = (RubyModule) source;
        } else {
            throw runtime.newTypeError(source + " is not a type/class");
        }

        Object constant = cachedConstant; // Store to temp so it does null out on us mid-stream
        if (!isCached(runtime, module, constant)) {
            constant = module.getConstant(getRef());
            // Recache
            if (constant != null) {
                generation = runtime.getConstantInvalidator().getData();
                hash = module.hashCode();
                cachedConstant = constant;
            }
        }
        return constant;
    }
}
