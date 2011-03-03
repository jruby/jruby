package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.MethAddr;

public class InlineTest implements CompilerPass {
    public final String methodToInline;

    public InlineTest(String m) { methodToInline = m; }

    public boolean isPreOrder()  { return true; }

    public void run(IRScope s) {
        if (s instanceof IRMethod) {
           CFG       c  = ((IRMethod)s).getCFG();
           IRModule m  = s.getNearestModule();
           IRMethod  mi = m.getInstanceMethod(methodToInline);
           for (BasicBlock b: c.getNodes()) {
               for (Instr i: b.getInstrs()) {
                   if (i instanceof CallInstr) {
                       CallInstr call = (CallInstr)i;
                       MethAddr addr = call.getMethodAddr();
                       if (methodToInline.equals(((MethAddr)addr).getName())) {
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
