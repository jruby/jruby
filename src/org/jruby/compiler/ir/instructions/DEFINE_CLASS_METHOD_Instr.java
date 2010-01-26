package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;

public class DEFINE_CLASS_METHOD_Instr extends NoOperandInstr {
    public final IR_Module _module; // class or module
    public final IRMethod _method;

    public DEFINE_CLASS_METHOD_Instr(IR_Module c, IRMethod m) {
        super(Operation.DEF_CLASS_METH);
        _module = c;
        _method = m;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + _module.getName() + ", "
                + _method.getName() + ")";
    }
}
