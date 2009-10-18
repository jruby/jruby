package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.Operation;

public class DEFINE_INSTANCE_METHOD_Instr extends NoOperandInstr
{
    public final IR_Module _classOrModule;
    public final IR_Method _method;

    public DEFINE_INSTANCE_METHOD_Instr(IR_Module c, IR_Method m)
    {
        super(Operation.DEF_INST_METH);
        _classOrModule = c;
        _method = m;
    }

    public String toString() { return super.toString() + "(" + _classOrModule._name + ", " + _method._name + ")"; }
}
