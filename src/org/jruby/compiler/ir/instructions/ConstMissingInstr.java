package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstMissingInstr extends Instr {
    IRScope scope;
    String  missingConst;

    public ConstMissingInstr(Variable dest, IRScope scope, String missingConst) {
        super(Operation.CONST_MISSING, dest);
        this.scope = scope;
        this.missingConst = missingConst;
    }

    public Operand[] getOperands() { 
        return new Operand[] {};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ConstMissingInstr(ii.getRenamedVariable(result), scope, missingConst);
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + scope.getName() + "," + missingConst  + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        StaticScope staticScope = scope.getStaticScope();
        Object constant = staticScope.getModule().callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(missingConst));
        getResult().store(interp, context, self, constant);
        
        return null;
    }
}
