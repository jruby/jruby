package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.representations.CFG;

public class IR_Printer implements CompilerPass {
    public IR_Printer() { }

    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder()  { return true; }

    public void run(IR_Scope s) {
        System.out.println("----------------------------------------");
        System.out.println(s.toString());

        // If the cfg of the method is around, print the CFG!
        CFG c = null;
        if (s instanceof IR_ExecutionScope)
            c = ((IR_ExecutionScope)s).getCFG();

        if (c != null) {
            System.out.println("\nGraph:\n" + c.getGraph().toString());
            System.out.println("\nInstructions:\n" + c.toStringInstrs());
        } else if (s instanceof IR_Method) {
            IR_Method m = (IR_Method)s;
            System.out.println("\n  instrs:\n" + m.toStringInstrs());
            System.out.println("\n  live variables:\n" + m.toStringVariables());
        }
    }
}
