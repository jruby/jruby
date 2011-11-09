package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

import org.jruby.RubyModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.

public class SearchConstInstr extends Instr implements ResultInstr {
    IRModule definingModule;
    String constName;
    private final Variable result;

    public SearchConstInstr(Variable result, IRModule definingModule, String constName) {
        super(Operation.SEARCH_CONST);
        
        this.definingModule = definingModule;
        this.constName = constName;
        this.result = result;
    }

    public Operand[] getOperands() { 
        return new Operand[] {};
    }
    
    public Variable getResult() {
        return result;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), definingModule, constName);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + (definingModule == null ? "-dynamic-" : definingModule.getName()) + "," + constName  + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        StaticScope staticScope = definingModule == null ? context.getCurrentScope().getStaticScope() : definingModule.getStaticScope();
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        Object constant;
        
        if (staticScope == null) { // FIXME: CORE CLASSES have no staticscope yet...hack for now
            constant = object.getConstant(constName);
        } else {
            constant = staticScope.getConstant(runtime, constName, object);
        }

        if (constant == null) constant = UndefinedValue.UNDEFINED;
        
        result.store(interp, context, self, constant);

        return null;
    }
}
