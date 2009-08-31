package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.CodeVersion;
import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;

// This instruction check that the method version (at the time of execution) is a specific value (at the time of compilation)
// If this check fails, control is transferred to a label where fixup code compiles a fresh de-optimized version of the method!
public class ASSERT_METHOD_VERSION_Instr extends IR_Instr
{
    IR_Class    _c;
    String      _m;
    CodeVersion _v;
    Label       _l;

    public ASSERT_METHOD_VERSION_Instr(IR_Class c, String methodName, CodeVersion currVersion, Label deoptLabel)
    {
        super(Operation.ASSERT_METHOD_VERSION);
        _c = c;
        _m = methodName;
        _v = currVersion;
        _l = deoptLabel;
    }

    public String toString() { return super.toString() + "(" + _c._name + ":" + _m + "=" + _v + ", " + _l + ")"; }

    public Operand[] getOperands() { return new Operand[]{}; }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }
}
