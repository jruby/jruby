package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
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

public class DefineModuleInstr extends Instr implements ResultInstr {
    private final IRModule newIRModule;
    private Operand container;
    private Variable result;

    public DefineModuleInstr(IRModule newIRModule, Variable result, Operand container) {
        super(Operation.DEF_MODULE);
        
        assert result != null : "DefineModuleInstr result is null";
        
        this.newIRModule = newIRModule;
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
        return super.toString() + "(" + container + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineModuleInstr(this.newIRModule, ii.getRenamedVariable(result), container.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, Object[] temp, Block block) {
        Object rubyContainer = container.retrieve(context, self, temp);
        
        if (!(rubyContainer instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");

        RubyModule newRubyModule = ((RubyModule) rubyContainer).defineOrGetModuleUnder(newIRModule.getName());
        newIRModule.getStaticScope().setModule(newRubyModule);
        DynamicMethod method = new InterpretedIRMethod(newIRModule.getRootMethod(), Visibility.PUBLIC, newRubyModule);

        // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineClass, DefineModule instrs?
        return method.call(context, newRubyModule, newRubyModule, "", new IRubyObject[]{}, block);
    }
}
