package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.Operation;

public class DEFINE_CLASS_METHOD_Instr extends NoOperandInstr
{
    public final IR_Module _module; // class or module
    public final IR_Method _method;

    public DEFINE_CLASS_METHOD_Instr(IR_Module c, IR_Method m)
    {
        super(Operation.DEF_CLASS_METH);
        _module = c;
        _method = m;
    }

    public String toString() { return super.toString() + "(" + _module._name + ", " + _method._name + ")"; }
}
