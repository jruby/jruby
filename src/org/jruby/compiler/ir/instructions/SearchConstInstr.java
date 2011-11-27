package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

import org.jruby.RubyModule;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.

public class SearchConstInstr extends Instr implements ResultInstr {
    IRModule definingModule;
    String constName;
    private Variable result;

    public SearchConstInstr(Variable result, IRModule definingModule, String constName) {
        super(Operation.SEARCH_CONST);
        
        assert result != null: "SearchConstInstr result is null";
        
        this.definingModule = definingModule;
        this.constName = constName;
        this.result = result;
    }

    public Operand[] getOperands() { 
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), definingModule, constName);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + (definingModule == null ? "-dynamic-" : definingModule.getName()) + "," + constName  + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        StaticScope staticScope = definingModule == null ? currDynScope.getStaticScope() : definingModule.getStaticScope();
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        Object constant;
        
        if (staticScope == null) { // FIXME: Object scope has no staticscope yet
            constant = object.getConstant(constName);
        } else {
            constant = staticScope.getConstant(runtime, constName, object);
        }

        if (constant == null) constant = UndefinedValue.UNDEFINED;
		  return constant;
    }
}
