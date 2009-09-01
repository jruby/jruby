package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.CodeVersion;
import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;

// This instruction check that the method version (at the time of execution) is a specific value (at the time of compilation)
// If this check fails, control is transferred to a label where fixup code compiles a fresh de-optimized version of the method!
public class ASSERT_METHOD_VERSION_Instr extends IR_Instr
{
    IR_Module   _module;
    String      _method;
    CodeVersion _version;
    Label       _label;

    public ASSERT_METHOD_VERSION_Instr(IR_Module module, String methodName, CodeVersion currVersion, Label deoptLabel)
    {
        super(Operation.ASSERT_METHOD_VERSION);
        _module = module;
        _method = methodName;
        _version = currVersion;
        _label = deoptLabel;
    }

    public String toString() { return super.toString() + "(" + _module._name + ":" + _method + "=" + _version + ", " + _label + ")"; }

    public Operand[] getOperands() { return new Operand[]{}; }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }
}
