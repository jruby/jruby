package org.jruby.compiler.ir.compiler_pass.opts;

import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.instructions.ASSERT_METHOD_VERSION_Instr;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.COPY_Instr;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.CodeVersion;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Float;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Constant;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class DeadCodeElimination implements CompilerPass
{
    public DeadCodeElimination() { }

    public boolean isPreOrder() { return false; }

    public void run(IR_Scope s)
    {
        if (!(s instanceof IR_Method))
            return;

        LiveVariablesProblem lvp = new LiveVariablesProblem();
//        System.out.println("------- Live variable analysis output for scope -------");
        lvp.compute_MOP_Solution(s.getCFG());
//        System.out.println(s.toString());
//        System.out.println(lvp.toString());
        lvp.markDeadInstructions();
    }
}
