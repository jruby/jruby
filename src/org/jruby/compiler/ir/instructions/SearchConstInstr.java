package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.

public class SearchConstInstr extends Instr implements ResultInstr {
    Operand definingScope;
    String constName;
    private Variable result;

    // Constant caching 
    private volatile transient Object cachedConstant = null;
    private Object generation = -1;

    public SearchConstInstr(Variable result, Operand definingScope, String constName) {
        super(Operation.SEARCH_CONST);
        
        assert result != null: "SearchConstInstr result is null";
        
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

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), definingScope, constName);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + definingScope + "," + constName  + ")";
    }

    private Object cache(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Ruby runtime, Object constant) {
        StaticScope staticScope = (StaticScope) definingScope.retrieve(context, self, currDynScope, temp);
        RubyModule object = runtime.getObject();
        if (staticScope == null) { // FIXME: Object scope has no staticscope yet
            constant = object.getConstant(constName);
        } else {
            constant = staticScope.getConstant(runtime, constName, object);
        }
        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        } else {
            // recache
            generation = runtime.getConstantInvalidator().getData();
            cachedConstant = constant;
        }
        return constant;
    }

    private boolean isCached(Ruby runtime, Object value) {
        return value != null && generation == runtime.getConstantInvalidator().getData();
    }
    
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.getRuntime();
        Object constant = cachedConstant; // Store to temp so it does null out on us mid-stream
        if (!isCached(runtime, constant)) constant = cache(context, currDynScope, self, temp, runtime, constant);

        return constant;
    }
}
