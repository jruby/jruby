package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.compiler.ir.IRMetaClass;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineMetaClassInstr extends Instr implements ResultInstr {
    private IRMetaClass dummyMetaClass;
    private Operand object;
    private Variable result;
    
    public DefineMetaClassInstr(Variable result, Operand object, IRMetaClass dummyMetaClass) {
        super(Operation.DEF_META_CLASS);
        
        assert result != null: "DefineMetaClassInstr result is null";
        
        this.dummyMetaClass = dummyMetaClass;
        this.object = object;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{object};
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        object = object.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + object + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineMetaClassInstr(ii.getRenamedVariable(result), object.cloneForInlining(ii), dummyMetaClass);
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.getRuntime();
        IRubyObject obj = (IRubyObject)object.retrieve(context, self, temp);
        
        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("no virtual class for " + obj.getMetaClass().getBaseName());
        } else {
            if (runtime.getSafeLevel() >= 4 && !obj.isTaint()) {
                throw runtime.newSecurityError("Insecure: can't extend object.");
            }
            
            RubyClass singletonClass = obj.getSingletonClass();
            dummyMetaClass.getStaticScope().setModule(singletonClass);
            DynamicMethod method = new InterpretedIRMethod(dummyMetaClass.getRootMethod(), Visibility.PUBLIC, singletonClass);
            // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineMetaClass instr?
            return method.call(context, singletonClass, singletonClass, "", new IRubyObject[]{}, block);
        }
    }
}
