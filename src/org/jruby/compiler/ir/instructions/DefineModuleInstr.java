package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModuleBody;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;

public class DefineModuleInstr extends Instr implements ResultInstr {
    private final IRModuleBody newIRModuleBody;
    private Operand container;
    private Variable result;

    public DefineModuleInstr(IRModuleBody newIRModuleBody, Variable result, Operand container) {
        super(Operation.DEF_MODULE);
        
        assert result != null : "DefineModuleInstr result is null";
        
        this.newIRModuleBody = newIRModuleBody;
        this.container = container;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{container};
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + newIRModuleBody.getName() + ", " + container + ", " + newIRModuleBody.getFileName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIxME: So, do we clone the module body scope or not?
        return new DefineModuleInstr(this.newIRModuleBody, ii.getRenamedVariable(result), container.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object rubyContainer = container.retrieve(context, self, currDynScope, temp);
        
        if (!(rubyContainer instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");

        RubyModule newRubyModule = ((RubyModule) rubyContainer).defineOrGetModuleUnder(newIRModuleBody.getName());
        newIRModuleBody.getStaticScope().setModule(newRubyModule);
        DynamicMethod method = new InterpretedIRMethod(newIRModuleBody, Visibility.PUBLIC, newRubyModule);

        // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineClass, DefineModule instrs?
        return method.call(context, newRubyModule, newRubyModule, "", new IRubyObject[]{}, block);
    }
}
