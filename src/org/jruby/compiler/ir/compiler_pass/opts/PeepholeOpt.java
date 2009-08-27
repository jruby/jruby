package org.jruby.compiler.ir.opts;

import java.util.Map;
import java.util.HashMap;

import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

public class PeepholeOpt implements Optimization
{
    public PeepholeOpt() { }

    public void run(IR_Method m)
    {
        Map<Operand,Operand> valueMap = new HashMap<Operand,Operand>();
        for (IR_Instr i : m.getInstrs()) {
            // Reset value map if this instruction is the start/end of a basic block
            //
            // Right now, calls are considered hard boundaries for optimization and
            // information cannot be propagated across them!
            //
            // SSS FIXME: Rather than treat all calls with a broad brush, what we need
            // is to capture different attributes about a call :
            //   - uses closures
            //   - known call target
            //   - can modify scope,
            //   - etc.
            //
            // This information is probably already present in the AST Inspector
            Operation iop = i._op;
            if (iop.startsBasicBlock() || iop.endsBasicBlock() || iop.isCall()) {
                valueMap = new HashMap<Operand,Operand>();
            }

            // Simplify instruction and record mapping between target variable and simplified value
            Operand val = i.simplifyAndGetResult(valueMap);
            Operand res = i.getResult();
            System.out.println("For " + i + "; dst = " + res + "; val = " + val);
            if (val != null && res != null && res != val)
                valueMap.put(res, val);
        }
    }
}
