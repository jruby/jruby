package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;

public class DEFINE_INSTANCE_METHOD_Instr extends NoOperandInstr {
    public final IR_Module _classOrModule;
    public final IRMethod _method;

    public DEFINE_INSTANCE_METHOD_Instr(IR_Module c, IRMethod m) {
        super(Operation.DEF_INST_METH);
        _classOrModule = c;
        _method = m;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + _classOrModule.getName() + ", " +
                _method.getName() + ")";
    }
}
