package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.Operation;

public class DEFINE_INSTANCE_METHOD_Instr extends NoOperandInstr
{
    public final IR_Class  _class;
    public final IR_Method _method;

    public DEFINE_INSTANCE_METHOD_Instr(IR_Class c, IR_Method m)
    {
        super(Operation.DEF_INST_METH);
        _class = c;
        _method = m;
    }

    public String toString() { return super.toString() + "(" + _class._name + ", " + _method._name + ")"; }
}
