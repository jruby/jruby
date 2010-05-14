package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.instructions.CallInstruction;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MethAddr;

public class InlineTest implements CompilerPass
{
    public final String methodToInline;

    public InlineTest(String m) { methodToInline = m; }

    public boolean isPreOrder()  { return true; }

    public void run(IR_Scope s) { 
        if (s instanceof IRMethod) {
           CFG       c  = ((IRMethod)s).getCFG();
           IR_Module m  = s.getNearestModule();
           IRMethod  mi = m.getInstanceMethod(methodToInline);
           for (BasicBlock b: c.getNodes()) {
               for (IR_Instr i: b.getInstrs()) {
                   if (i instanceof CallInstruction) {
                       CallInstruction call = (CallInstruction)i;
                       Operand addr = call.getMethodAddr();
                       if ((addr instanceof MethAddr) && methodToInline.equals(((MethAddr)addr).getName())) {
                           System.out.println("Will be inlining method " + methodToInline + " at callsite: " + call);
                           c.inlineMethod(mi, b, call);
                           // Just inline once per scope -- this is a test after all!
                           // Because, the surrounding iterators will break with a concurrent modification exception if we proceed!
                           return;
                       }
                   }
               }
           }
        }
    }
}
